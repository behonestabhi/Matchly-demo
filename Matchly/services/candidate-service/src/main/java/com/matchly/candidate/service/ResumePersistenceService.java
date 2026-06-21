package com.matchly.candidate.service;

import com.matchly.candidate.client.AiDtos.ParseResponse;
import com.matchly.candidate.domain.CandidateSkill;
import com.matchly.candidate.domain.Certification;
import com.matchly.candidate.domain.Education;
import com.matchly.candidate.domain.Experience;
import com.matchly.candidate.domain.ParsedResume;
import com.matchly.candidate.domain.Project;
import com.matchly.candidate.domain.Resume;
import com.matchly.candidate.exception.NotFoundException;
import com.matchly.candidate.repository.CandidateSkillRepository;
import com.matchly.candidate.repository.CertificationRepository;
import com.matchly.candidate.repository.EducationRepository;
import com.matchly.candidate.repository.ExperienceRepository;
import com.matchly.candidate.repository.ParsedResumeRepository;
import com.matchly.candidate.repository.ProjectRepository;
import com.matchly.candidate.repository.ResumeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional persistence for the parse flow. Separated from
 * {@link ResumeParseOrchestrator} so each DB step commits in its own transaction
 * (the orchestrator runs on a worker thread, outside any request transaction).
 */
@Service
public class ResumePersistenceService {

    private static final Logger log = LoggerFactory.getLogger(ResumePersistenceService.class);

    private final ResumeRepository resumeRepository;
    private final ParsedResumeRepository parsedResumeRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final ProjectRepository projectRepository;
    private final CertificationRepository certificationRepository;

    public ResumePersistenceService(ResumeRepository resumeRepository,
                                    ParsedResumeRepository parsedResumeRepository,
                                    CandidateSkillRepository candidateSkillRepository,
                                    ExperienceRepository experienceRepository,
                                    EducationRepository educationRepository,
                                    ProjectRepository projectRepository,
                                    CertificationRepository certificationRepository) {
        this.resumeRepository = resumeRepository;
        this.parsedResumeRepository = parsedResumeRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.projectRepository = projectRepository;
        this.certificationRepository = certificationRepository;
    }

    /** Persist the structured parse result into the §2 tables. */
    @Transactional
    public void persistStructuredData(UUID candidateId, UUID resumeId, ParseResponse parsed) {
        // parsed_resumes (1:1 with resume)
        ParsedResume pr = parsedResumeRepository.findByResumeId(resumeId)
                .orElseGet(() -> new ParsedResume(UUID.randomUUID(), resumeId));
        pr.setFullName(parsed.fullName());
        pr.setEmail(parsed.email());
        pr.setPhone(parsed.phone());
        pr.setTotalExpYrs(parsed.totalExpYrs() == null
                ? null : BigDecimal.valueOf(parsed.totalExpYrs()));
        pr.setRawText(parsed.rawText());
        parsedResumeRepository.save(pr);

        // candidate_skills (sourced from RESUME). De-dup by skill name.
        if (parsed.skills() != null) {
            for (String skill : parsed.skills()) {
                if (skill == null || skill.isBlank()) {
                    continue;
                }
                // Deterministic skill_id from the name so re-parses upsert rather than duplicate.
                UUID skillId = UUID.nameUUIDFromBytes(
                        skill.trim().toLowerCase().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                candidateSkillRepository.save(
                        new CandidateSkill(candidateId, skillId, skill.trim(), "RESUME"));
            }
        }

        // experiences
        if (parsed.experiences() != null) {
            for (var e : parsed.experiences()) {
                Experience exp = new Experience(UUID.randomUUID(), resumeId);
                exp.setCompany(e.company());
                exp.setTitle(e.title());
                exp.setStartDate(parseDate(e.startDate()));
                exp.setEndDate(parseDate(e.endDate()));
                exp.setDescription(e.description());
                experienceRepository.save(exp);
            }
        }

        // education
        if (parsed.education() != null) {
            for (var ed : parsed.education()) {
                Education edu = new Education(UUID.randomUUID(), resumeId);
                edu.setInstitution(ed.institution());
                edu.setDegree(ed.degree());
                edu.setField(ed.field());
                edu.setLevel(ed.level());
                edu.setStartYear(ed.startYear());
                edu.setEndYear(ed.endYear());
                educationRepository.save(edu);
            }
        }

        // projects
        if (parsed.projects() != null) {
            for (var p : parsed.projects()) {
                Project proj = new Project(UUID.randomUUID(), resumeId);
                proj.setName(p.name());
                proj.setDescription(p.description());
                proj.setTechStack(p.techStack());
                projectRepository.save(proj);
            }
        }

        // certifications
        if (parsed.certifications() != null) {
            for (var c : parsed.certifications()) {
                Certification cert = new Certification(UUID.randomUUID(), resumeId);
                cert.setName(c.name());
                cert.setIssuer(c.issuer());
                cert.setIssuedAt(parseDate(c.issuedAt()));
                certificationRepository.save(cert);
            }
        }
    }

    /** Mark a resume PARSED and record its embedding ref. */
    @Transactional
    public void markParsed(UUID resumeId, UUID embeddingRef) {
        Resume resume = requireResume(resumeId);
        resume.setParseStatus("PARSED");
        resume.setEmbeddingRef(embeddingRef);
        resumeRepository.save(resume);
    }

    /** Mark a resume FAILED (AI unavailable or parse error). */
    @Transactional
    public void markFailed(UUID resumeId) {
        try {
            Resume resume = requireResume(resumeId);
            resume.setParseStatus("FAILED");
            resumeRepository.save(resume);
        } catch (RuntimeException ex) {
            log.error("Could not mark resume {} FAILED", resumeId, ex);
        }
    }

    private Resume requireResume(UUID resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found: " + resumeId));
    }

    /**
     * Parse a date that may be a full ISO date ({@code 2021-06-01}) or a bare year
     * ({@code 2021}); returns null if it cannot be interpreted.
     */
    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        try {
            return LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignored) {
            // fall through
        }
        // year-month
        try {
            if (v.matches("\\d{4}-\\d{2}")) {
                return LocalDate.parse(v + "-01", DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (Exception ignored) {
            // fall through
        }
        // bare year
        try {
            if (v.matches("\\d{4}")) {
                return LocalDate.of(Integer.parseInt(v), 1, 1);
            }
        } catch (Exception ignored) {
            // fall through
        }
        log.debug("Unparseable date '{}' ignored", value);
        return null;
    }
}
