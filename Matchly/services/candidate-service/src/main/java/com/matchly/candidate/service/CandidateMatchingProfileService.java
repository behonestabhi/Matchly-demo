package com.matchly.candidate.service;

import com.matchly.candidate.domain.Education;
import com.matchly.candidate.domain.ParsedResume;
import com.matchly.candidate.domain.Resume;
import com.matchly.candidate.dto.CandidateMatchingProfileResponse;
import com.matchly.candidate.repository.CandidateSkillRepository;
import com.matchly.candidate.repository.EducationRepository;
import com.matchly.candidate.repository.ParsedResumeRepository;
import com.matchly.candidate.repository.ResumeRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles the {@link CandidateMatchingProfileResponse} consumed by the matching
 * service: skills, experience, education level, the resume embedding ref, and a
 * (capped) resume text for on-the-fly semantic scoring.
 */
@Service
@Transactional(readOnly = true)
public class CandidateMatchingProfileService {

    /** Cap on resume text returned to matching (semantic scoring needs signal, not the whole doc). */
    private static final int MAX_TEXT_CHARS = 5000;

    /** Education level ranking, to pick the highest degree. */
    private static final Map<String, Integer> EDU_RANK = Map.of(
            "HS", 1, "DIPLOMA", 2, "BACHELOR", 3, "MASTER", 4, "PHD", 5);

    private final ResumeRepository resumeRepository;
    private final ParsedResumeRepository parsedResumeRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final EducationRepository educationRepository;

    public CandidateMatchingProfileService(ResumeRepository resumeRepository,
                                           ParsedResumeRepository parsedResumeRepository,
                                           CandidateSkillRepository candidateSkillRepository,
                                           EducationRepository educationRepository) {
        this.resumeRepository = resumeRepository;
        this.parsedResumeRepository = parsedResumeRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.educationRepository = educationRepository;
    }

    public CandidateMatchingProfileResponse assemble(UUID candidateId) {
        List<String> skills = candidateSkillRepository.findByIdCandidateId(candidateId).stream()
                .map(s -> s.getSkillName())
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .toList();

        Resume resume = pickPrimaryResume(candidateId);
        if (resume == null) {
            return new CandidateMatchingProfileResponse(candidateId, skills, null, null, null, null);
        }

        Optional<ParsedResume> parsed = parsedResumeRepository.findByResumeId(resume.getId());
        Double totalExpYrs = parsed.map(ParsedResume::getTotalExpYrs)
                .map(BigDecimal::doubleValue)
                .orElse(null);
        String resumeText = parsed.map(ParsedResume::getRawText)
                .map(this::cap)
                .orElse(null);
        String educationLevel = highestEducationLevel(resume.getId());

        return new CandidateMatchingProfileResponse(
                candidateId, skills, totalExpYrs, educationLevel, resume.getEmbeddingRef(), resumeText);
    }

    private Resume pickPrimaryResume(UUID candidateId) {
        List<Resume> resumes = resumeRepository.findByCandidateIdOrderByCreatedAtDesc(candidateId);
        return resumes.stream()
                .filter(Resume::isPrimary)
                .findFirst()
                .orElse(resumes.isEmpty() ? null : resumes.get(0));
    }

    private String highestEducationLevel(UUID resumeId) {
        return educationRepository.findByResumeId(resumeId).stream()
                .map(Education::getLevel)
                .filter(l -> l != null && !l.isBlank())
                .map(l -> l.trim().toUpperCase())
                .max((a, b) -> Integer.compare(
                        EDU_RANK.getOrDefault(a, 0), EDU_RANK.getOrDefault(b, 0)))
                .orElse(null);
    }

    private String cap(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= MAX_TEXT_CHARS ? text : text.substring(0, MAX_TEXT_CHARS);
    }
}
