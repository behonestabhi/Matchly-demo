package com.matchly.matching.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchly.matching.client.AiScoreClient;
import com.matchly.matching.client.AiDtos.PredictRequest;
import com.matchly.matching.client.AiDtos.PredictResponse;
import com.matchly.matching.client.AiDtos.ScoreRequest;
import com.matchly.matching.client.AiDtos.ScoreResponse;
import com.matchly.matching.client.AiDtos.SkillGapRequest;
import com.matchly.matching.client.AiDtos.SkillGapResponse;
import com.matchly.matching.client.CandidateClient;
import com.matchly.matching.client.CandidateData;
import com.matchly.matching.client.JobClient;
import com.matchly.matching.client.JobData;
import com.matchly.matching.config.MatchingProperties;
import com.matchly.matching.domain.MatchScore;
import com.matchly.matching.domain.SkillGap;
import com.matchly.matching.domain.SuccessPrediction;
import com.matchly.matching.dto.MatchScoreView;
import com.matchly.matching.dto.PageResponse;
import com.matchly.matching.dto.PredictionView;
import com.matchly.matching.dto.SkillGapView;
import com.matchly.matching.event.MatchingEventPayloads.MatchScored;
import com.matchly.matching.event.MatchingEventPayloads.SkillGapComputed;
import com.matchly.matching.event.MatchingEventPublisher;
import com.matchly.matching.repository.MatchScoreRepository;
import com.matchly.matching.repository.SkillGapRepository;
import com.matchly.matching.repository.SuccessPredictionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates candidate↔job scoring: fetches candidate/job data from their
 * services, calls the AI service to score, persists results, and emits events.
 * Reads of persisted scores/rankings never require the AI service to be up;
 * only on-demand compute does (and it degrades to 503 when a dependency is down).
 */
@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private static final Map<String, Integer> EDU_RANK = Map.of(
            "HS", 1, "DIPLOMA", 2, "BACHELOR", 3, "MASTER", 4, "PHD", 5);

    private final CandidateClient candidateClient;
    private final JobClient jobClient;
    private final AiScoreClient aiScoreClient;
    private final MatchScoreRepository matchScoreRepository;
    private final SkillGapRepository skillGapRepository;
    private final SuccessPredictionRepository predictionRepository;
    private final MatchingEventPublisher eventPublisher;
    private final MatchingProperties props;
    private final ObjectMapper objectMapper;

    public MatchingService(CandidateClient candidateClient,
                           JobClient jobClient,
                           AiScoreClient aiScoreClient,
                           MatchScoreRepository matchScoreRepository,
                           SkillGapRepository skillGapRepository,
                           SuccessPredictionRepository predictionRepository,
                           MatchingEventPublisher eventPublisher,
                           MatchingProperties props,
                           ObjectMapper objectMapper) {
        this.candidateClient = candidateClient;
        this.jobClient = jobClient;
        this.aiScoreClient = aiScoreClient;
        this.matchScoreRepository = matchScoreRepository;
        this.skillGapRepository = skillGapRepository;
        this.predictionRepository = predictionRepository;
        this.eventPublisher = eventPublisher;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    /**
     * Score a candidate against a job, persist the score + skill gap, and emit
     * {@code MatchScored} / {@code SkillGapComputed}. Backs both the
     * {@code ApplicationSubmitted} consumer and on-demand scoring.
     */
    @Transactional
    public MatchScore computeAndStore(UUID candidateId, UUID jobId, String correlationId) {
        CandidateData candidate = candidateClient.fetch(candidateId);
        JobData job = jobClient.fetch(jobId);

        List<String> jobSkills = job.requiredSkills().stream()
                .map(JobData.RequiredSkill::skillName)
                .filter(s -> s != null && !s.isBlank())
                .toList();

        ScoreRequest request = new ScoreRequest(
                candidate.resumeText(),
                job.description(),
                candidate.embeddingRef() == null ? null : candidate.embeddingRef().toString(),
                job.embeddingRef() == null ? null : job.embeddingRef().toString(),
                candidate.skills(),
                jobSkills,
                candidate.totalExpYrs(),
                job.minExpYrs(),
                job.maxExpYrs(),
                candidate.educationLevel(),
                job.educationLevel());

        ScoreResponse resp = aiScoreClient.score(request);
        String modelVersion = resp.modelVersion() != null ? resp.modelVersion()
                : props.getScoring().getModelVersion();
        Instant now = Instant.now();

        MatchScore score = matchScoreRepository
                .findByCandidateIdAndJobIdAndModelVersion(candidateId, jobId, modelVersion)
                .map(existing -> {
                    existing.setFinalScore(resp.finalScore());
                    existing.setSemanticScore(resp.semantic());
                    existing.setSkillOverlap(resp.skillOverlap());
                    existing.setExperienceFit(resp.experienceFit());
                    existing.setEducationFit(resp.educationFit());
                    existing.setScoredAt(now);
                    return existing;
                })
                .orElseGet(() -> new MatchScore(
                        UUID.randomUUID(), candidateId, jobId, resp.finalScore(), resp.semantic(),
                        resp.skillOverlap(), resp.experienceFit(), resp.educationFit(), modelVersion, now));
        matchScoreRepository.save(score);

        List<String> missing = resp.missingSkills() == null ? List.of() : resp.missingSkills();
        persistSkillGap(candidateId, jobId, missing, null, now);

        eventPublisher.publish(candidateId.toString(), "MatchScored",
                new MatchScored(candidateId, jobId, resp.finalScore(), modelVersion), correlationId);
        eventPublisher.publish(candidateId.toString(), "SkillGapComputed",
                new SkillGapComputed(candidateId, jobId, missing), correlationId);

        log.info("Scored candidate {} vs job {} = {} ({})", candidateId, jobId, resp.finalScore(), modelVersion);
        return score;
    }

    /** Latest persisted score for a pair, computing on demand if none exists. */
    @Transactional
    public MatchScoreView getScore(UUID candidateId, UUID jobId) {
        return matchScoreRepository.findFirstByCandidateIdAndJobIdOrderByScoredAtDesc(candidateId, jobId)
                .map(MatchScoreView::from)
                .orElseGet(() -> MatchScoreView.from(computeAndStore(candidateId, jobId, null)));
    }

    @Transactional(readOnly = true)
    public PageResponse<MatchScoreView> rankedCandidates(UUID jobId, int page, int size) {
        Page<MatchScore> p = matchScoreRepository
                .findByJobIdOrderByFinalScoreDesc(jobId, pageable(page, size));
        return PageResponse.of(p, MatchScoreView::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<MatchScoreView> jobsForCandidate(UUID candidateId, int page, int size) {
        Page<MatchScore> p = matchScoreRepository
                .findByCandidateIdOrderByFinalScoreDesc(candidateId, pageable(page, size));
        return PageResponse.of(p, MatchScoreView::from);
    }

    /** Persisted skill gap, computing (with roadmap) on demand if absent. */
    @Transactional
    public SkillGapView getSkillGap(UUID candidateId, UUID jobId) {
        SkillGap existing = skillGapRepository.findByCandidateIdAndJobId(candidateId, jobId).orElse(null);
        if (existing != null && existing.getRoadmap() != null) {
            return toView(existing);
        }
        CandidateData candidate = candidateClient.fetch(candidateId);
        JobData job = jobClient.fetch(jobId);
        List<String> jobSkills = job.requiredSkills().stream()
                .map(JobData.RequiredSkill::skillName)
                .filter(s -> s != null && !s.isBlank())
                .toList();
        SkillGapResponse resp = aiScoreClient.skillGap(
                new SkillGapRequest(candidate.skills(), jobSkills, true));
        List<String> missing = resp.missingSkills() == null ? List.of() : resp.missingSkills();
        SkillGap saved = persistSkillGap(candidateId, jobId, missing, resp.roadmap(), Instant.now());
        return toView(saved);
    }

    /** Success prediction for a pair (always recomputed against the latest features). */
    @Transactional
    public PredictionView predict(UUID candidateId, UUID jobId) {
        CandidateData candidate = candidateClient.fetch(candidateId);
        JobData job = jobClient.fetch(jobId);

        double semanticScore = matchScoreRepository
                .findFirstByCandidateIdAndJobIdOrderByScoredAtDesc(candidateId, jobId)
                .map(MatchScore::getSemanticScore)
                .map(BigDecimal::doubleValue)
                .orElse(0.0);

        Map<String, Object> features = new LinkedHashMap<>();
        features.put("yearsExperience", candidate.totalExpYrs() != null ? candidate.totalExpYrs() : 0.0);
        features.put("skillMatchCount", skillMatchCount(candidate.skills(), job));
        features.put("certificationCount", 0);
        features.put("educationLevel", eduRank(candidate.educationLevel()));
        features.put("projectCount", 0);
        features.put("semanticScore", semanticScore);

        PredictResponse resp = aiScoreClient.predict(new PredictRequest(features));
        String modelVersion = resp.modelVersion() != null ? resp.modelVersion() : "predict-v1.0";
        SuccessPrediction prediction = new SuccessPrediction(
                UUID.randomUUID(), candidateId, jobId, resp.probability(), modelVersion,
                writeJson(features), Instant.now());
        predictionRepository.save(prediction);
        return PredictionView.from(prediction);
    }

    /** Re-score every known candidate↔job pair for a job (e.g. after a JD change). */
    @Transactional
    public int rescoreJob(UUID jobId, String correlationId) {
        List<MatchScore> existing = matchScoreRepository
                .findByJobIdOrderByFinalScoreDesc(jobId, Pageable.unpaged()).getContent();
        Set<UUID> candidates = new HashSet<>();
        existing.forEach(s -> candidates.add(s.getCandidateId()));
        candidates.forEach(candidateId -> {
            try {
                computeAndStore(candidateId, jobId, correlationId);
            } catch (RuntimeException ex) {
                log.warn("Rescore failed for candidate {} job {}: {}", candidateId, jobId, ex.getMessage());
            }
        });
        return candidates.size();
    }

    // ---- helpers --------------------------------------------------------- //

    private SkillGap persistSkillGap(UUID candidateId, UUID jobId, List<String> missing,
                                     JsonNode roadmap, Instant when) {
        SkillGap gap = skillGapRepository.findByCandidateIdAndJobId(candidateId, jobId)
                .orElseGet(() -> new SkillGap(UUID.randomUUID(), candidateId, jobId,
                        writeJson(missing), null, when));
        gap.setMissingSkills(writeJson(missing));
        if (roadmap != null && !roadmap.isNull()) {
            gap.setRoadmap(writeJson(roadmap));
        }
        gap.setGeneratedAt(when);
        return skillGapRepository.save(gap);
    }

    private SkillGapView toView(SkillGap gap) {
        return new SkillGapView(
                gap.getCandidateId(),
                gap.getJobId(),
                readStringList(gap.getMissingSkills()),
                readJson(gap.getRoadmap()),
                gap.getGeneratedAt());
    }

    private int skillMatchCount(List<String> candidateSkills, JobData job) {
        if (candidateSkills == null || candidateSkills.isEmpty()) {
            return 0;
        }
        Set<String> have = new HashSet<>();
        candidateSkills.forEach(s -> have.add(s.toLowerCase()));
        int count = 0;
        for (JobData.RequiredSkill rs : job.requiredSkills()) {
            if (rs.skillName() != null && have.contains(rs.skillName().toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    private static int eduRank(String level) {
        if (level == null) {
            return 0;
        }
        return EDU_RANK.getOrDefault(level.trim().toUpperCase(), 0);
    }

    private static Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize JSON: {}", e.getMessage());
            return "null";
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private JsonNode readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
