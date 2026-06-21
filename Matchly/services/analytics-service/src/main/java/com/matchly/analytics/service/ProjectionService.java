package com.matchly.analytics.service;

import com.matchly.analytics.domain.ConversionMetrics;
import com.matchly.analytics.domain.ConversionMetricsId;
import com.matchly.analytics.domain.FunnelDaily;
import com.matchly.analytics.domain.FunnelDailyId;
import com.matchly.analytics.domain.SkillDemand;
import com.matchly.analytics.domain.SkillDemandId;
import com.matchly.analytics.domain.TimeToHire;
import com.matchly.analytics.repository.ConversionMetricsRepository;
import com.matchly.analytics.repository.FunnelDailyRepository;
import com.matchly.analytics.repository.SkillDemandRepository;
import com.matchly.analytics.repository.TimeToHireRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies domain events to the denormalized read models (funnel, time-to-hire,
 * skill demand, conversion). All day-bucketing uses UTC ({@link ZoneOffset#UTC}).
 */
@Service
public class ProjectionService {

    private static final Logger log = LoggerFactory.getLogger(ProjectionService.class);

    private final FunnelDailyRepository funnelDailyRepository;
    private final TimeToHireRepository timeToHireRepository;
    private final SkillDemandRepository skillDemandRepository;
    private final ConversionMetricsRepository conversionMetricsRepository;

    public ProjectionService(FunnelDailyRepository funnelDailyRepository,
                             TimeToHireRepository timeToHireRepository,
                             SkillDemandRepository skillDemandRepository,
                             ConversionMetricsRepository conversionMetricsRepository) {
        this.funnelDailyRepository = funnelDailyRepository;
        this.timeToHireRepository = timeToHireRepository;
        this.skillDemandRepository = skillDemandRepository;
        this.conversionMetricsRepository = conversionMetricsRepository;
    }

    // ------------------------------------------------------------------ //
    // application.events
    // ------------------------------------------------------------------ //

    /** ApplicationSubmitted: a new application entered the funnel at APPLIED. */
    @Transactional
    public void onApplicationSubmitted(UUID applicationId, UUID candidateId, UUID jobId, Instant occurredAt) {
        if (jobId == null) {
            return;
        }
        LocalDate day = toUtcDay(occurredAt);
        funnel(jobId, day).incrementStage("APPLIED");

        // Seed the time-to-hire row (hired_at filled later on OFFER).
        if (applicationId != null && !timeToHireRepository.existsById(applicationId)) {
            timeToHireRepository.save(new TimeToHire(applicationId, jobId, occurredAt));
        }

        ConversionMetrics cm = conversion(jobId, day);
        cm.setApplications(cm.getApplications() + 1);
        recomputeConversion(jobId, cm);

        log.debug("ApplicationSubmitted: job={} day={} (application={})", jobId, day, applicationId);
    }

    /** StageChanged: bump the destination-stage funnel count; on OFFER compute time-to-hire. */
    @Transactional
    public void onStageChanged(UUID applicationId, UUID jobId, String fromStage, String toStage, Instant occurredAt) {
        LocalDate day = toUtcDay(occurredAt);

        UUID resolvedJobId = jobId;
        if (resolvedJobId == null && applicationId != null) {
            resolvedJobId = timeToHireRepository.findById(applicationId)
                    .map(TimeToHire::getJobId)
                    .orElse(null);
        }
        if (resolvedJobId == null) {
            log.warn("StageChanged for application {} has no resolvable jobId; skipping funnel update", applicationId);
            return;
        }

        funnel(resolvedJobId, day).incrementStage(toStage);

        if ("OFFER".equalsIgnoreCase(toStage) && applicationId != null) {
            TimeToHire tth = timeToHireRepository.findById(applicationId).orElse(null);
            if (tth == null) {
                tth = new TimeToHire(applicationId, resolvedJobId, occurredAt);
            }
            tth.setHiredAt(occurredAt);
            if (tth.getAppliedAt() != null) {
                double days = Duration.between(tth.getAppliedAt(), occurredAt).toMinutes() / 1440.0;
                tth.setDaysToHire(BigDecimal.valueOf(Math.max(days, 0.0)).setScale(2, RoundingMode.HALF_UP));
            }
            timeToHireRepository.save(tth);
        }

        recomputeConversion(resolvedJobId, conversion(resolvedJobId, day));
        log.debug("StageChanged: job={} {}->{} day={}", resolvedJobId, fromStage, toStage, day);
    }

    // ------------------------------------------------------------------ //
    // job.events
    // ------------------------------------------------------------------ //

    /** JobPosted: tally each required skill into skill_demand for the day. */
    @Transactional
    public void onJobPosted(UUID jobId, List<SkillRef> requiredSkills, Instant occurredAt) {
        LocalDate day = toUtcDay(occurredAt);
        for (SkillRef skill : requiredSkills) {
            if (skill == null || skill.name() == null || skill.name().isBlank()) {
                continue;
            }
            UUID skillId = skill.id() != null ? skill.id() : deterministicSkillId(skill.name());
            SkillDemandId id = new SkillDemandId(day, skillId);
            SkillDemand sd = skillDemandRepository.findById(id)
                    .orElseGet(() -> skillDemandRepository.save(new SkillDemand(day, skillId, skill.name())));
            sd.increment();
        }
        log.debug("JobPosted: job={} day={} skills={}", jobId, day, requiredSkills.size());
    }

    // ------------------------------------------------------------------ //
    // matching.events / interview.events — currently no dedicated read model.
    // ------------------------------------------------------------------ //

    /** MatchScored: recorded for completeness; no read-model column today. */
    public void onMatchScored(UUID candidateId, UUID jobId, Double finalScore) {
        log.debug("MatchScored observed: candidate={} job={} score={} (no projection)", candidateId, jobId, finalScore);
    }

    /** QuestionsGenerated: recorded for completeness; no read-model column today. */
    public void onQuestionsGenerated(UUID candidateId, UUID jobId, UUID questionSetId) {
        log.debug("QuestionsGenerated observed: candidate={} job={} set={} (no projection)",
                candidateId, jobId, questionSetId);
    }

    // ------------------------------------------------------------------ //
    // helpers
    // ------------------------------------------------------------------ //

    /** Skill reference from a JobPosted payload. */
    public record SkillRef(UUID id, String name) {
    }

    private FunnelDaily funnel(UUID jobId, LocalDate day) {
        return funnelDailyRepository.findById(new FunnelDailyId(jobId, day))
                .orElseGet(() -> funnelDailyRepository.save(new FunnelDaily(jobId, day)));
    }

    private ConversionMetrics conversion(UUID jobId, LocalDate day) {
        return conversionMetricsRepository.findById(new ConversionMetricsId(jobId, day))
                .orElseGet(() -> conversionMetricsRepository.save(new ConversionMetrics(jobId, day)));
    }

    /**
     * Recompute shortlist/offer rates for a (job, day) from that day's funnel
     * counts: shortlistRate = shortlisted/applications, offerRate = offer/applications.
     */
    private void recomputeConversion(UUID jobId, ConversionMetrics cm) {
        FunnelDaily f = funnelDailyRepository.findById(new FunnelDailyId(jobId, cm.getDay())).orElse(null);
        int apps = cm.getApplications() > 0 ? cm.getApplications() : (f == null ? 0 : f.getApplied());
        if (f == null || apps <= 0) {
            cm.setShortlistRate(BigDecimal.ZERO);
            cm.setOfferRate(BigDecimal.ZERO);
            return;
        }
        cm.setShortlistRate(rate(f.getShortlisted(), apps));
        cm.setOfferRate(rate(f.getOffer(), apps));
    }

    private BigDecimal rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal r = BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 3, RoundingMode.HALF_UP);
        // Clamp to NUMERIC(4,3) domain [0.000, 1.000].
        if (r.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE.setScale(3, RoundingMode.HALF_UP);
        }
        return r;
    }

    /** UTC day bucket for an instant (null-safe — defaults to now). */
    private LocalDate toUtcDay(Instant occurredAt) {
        Instant when = occurredAt != null ? occurredAt : Instant.now();
        return when.atZone(ZoneOffset.UTC).toLocalDate();
    }

    /**
     * Deterministic surrogate skill id when the event carries no canonical id, so
     * the same skill name always maps to the same {@code skill_id} (UUIDv3 over
     * the lower-cased name).
     */
    private UUID deterministicSkillId(String skillName) {
        return UUID.nameUUIDFromBytes(("skill:" + skillName.trim().toLowerCase())
                .getBytes(StandardCharsets.UTF_8));
    }
}
