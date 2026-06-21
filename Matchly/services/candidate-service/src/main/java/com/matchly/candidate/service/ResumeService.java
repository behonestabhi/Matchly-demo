package com.matchly.candidate.service;

import com.matchly.candidate.domain.Certification;
import com.matchly.candidate.domain.Education;
import com.matchly.candidate.domain.Experience;
import com.matchly.candidate.domain.ParsedResume;
import com.matchly.candidate.domain.Project;
import com.matchly.candidate.domain.Resume;
import com.matchly.candidate.dto.ResumeDetailResponse;
import com.matchly.candidate.dto.ResumeDetailResponse.CertificationDto;
import com.matchly.candidate.dto.ResumeDetailResponse.EducationDto;
import com.matchly.candidate.dto.ResumeDetailResponse.ExperienceDto;
import com.matchly.candidate.dto.ResumeDetailResponse.ParsedCandidate;
import com.matchly.candidate.dto.ResumeDetailResponse.ProjectDto;
import com.matchly.candidate.events.CandidateEventPublisher;
import com.matchly.candidate.exception.NotFoundException;
import com.matchly.candidate.repository.CandidateSkillRepository;
import com.matchly.candidate.repository.CertificationRepository;
import com.matchly.candidate.repository.EducationRepository;
import com.matchly.candidate.repository.ExperienceRepository;
import com.matchly.candidate.repository.ParsedResumeRepository;
import com.matchly.candidate.repository.ProjectRepository;
import com.matchly.candidate.repository.ResumeRepository;
import com.matchly.candidate.storage.ResumeStorage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Resume upload + lookup. Upload is synchronous up to persisting a PENDING
 * resume and emitting {@code ResumeUploaded}; the heavy parse work is handed to
 * {@link ResumeParseOrchestrator} which runs asynchronously and returns 202 to
 * the caller immediately.
 */
@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository resumeRepository;
    private final ParsedResumeRepository parsedResumeRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final ProjectRepository projectRepository;
    private final CertificationRepository certificationRepository;
    private final ResumeStorage storage;
    private final CandidateEventPublisher eventPublisher;
    private final ResumeParseOrchestrator parseOrchestrator;

    public ResumeService(ResumeRepository resumeRepository,
                         ParsedResumeRepository parsedResumeRepository,
                         CandidateSkillRepository candidateSkillRepository,
                         ExperienceRepository experienceRepository,
                         EducationRepository educationRepository,
                         ProjectRepository projectRepository,
                         CertificationRepository certificationRepository,
                         ResumeStorage storage,
                         CandidateEventPublisher eventPublisher,
                         ResumeParseOrchestrator parseOrchestrator) {
        this.resumeRepository = resumeRepository;
        this.parsedResumeRepository = parsedResumeRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.projectRepository = projectRepository;
        this.certificationRepository = certificationRepository;
        this.storage = storage;
        this.eventPublisher = eventPublisher;
        this.parseOrchestrator = parseOrchestrator;
    }

    /**
     * Store the file, create a PENDING resume row, emit {@code ResumeUploaded},
     * then kick off async parsing. Returns the new resume id.
     */
    @Transactional
    public UUID upload(UUID candidateId, MultipartFile file, String correlationId) {
        UUID resumeId = UUID.randomUUID();
        String s3Key = storage.store(candidateId, resumeId, file);
        String mimeType = file.getContentType() != null
                ? file.getContentType() : "application/octet-stream";
        String fileName = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : (resumeId + ".bin");

        Resume resume = new Resume(resumeId, candidateId, s3Key, fileName, mimeType);
        resume.setParseStatus("PENDING");
        resumeRepository.save(resume);

        // Emit ResumeUploaded → candidate.events (key = candidateId).
        eventPublisher.publishResumeUploaded(
                candidateId, resumeId, s3Key, fileName, mimeType, correlationId);

        // Read bytes once (within tx) and hand off to async parsing.
        byte[] bytes = storage.read(s3Key);
        parseOrchestrator.parseAsync(candidateId, resumeId, bytes, mimeType, correlationId);

        log.info("Resume {} uploaded for candidate {} (s3Key={}), parsing async",
                resumeId, candidateId, s3Key);
        return resumeId;
    }

    /** Build the resume detail view; structured data is populated only when PARSED. */
    @Transactional(readOnly = true)
    public ResumeDetailResponse getDetail(UUID resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found: " + resumeId));

        boolean parsed = "PARSED".equals(resume.getParseStatus());

        ParsedCandidate candidate = null;
        Optional<ParsedResume> pr = parsedResumeRepository.findByResumeId(resumeId);
        if (pr.isPresent()) {
            ParsedResume p = pr.get();
            candidate = new ParsedCandidate(
                    p.getFullName(),
                    p.getEmail(),
                    p.getTotalExpYrs() == null ? null : p.getTotalExpYrs().doubleValue());
        }

        List<String> skills = parsed
                ? candidateSkillRepository.findByIdCandidateId(resume.getCandidateId()).stream()
                        .map(s -> s.getSkillName()).toList()
                : List.of();

        List<ExperienceDto> experiences = parsed
                ? experienceRepository.findByResumeId(resumeId).stream()
                        .map(ResumeService::toExperienceDto).toList()
                : List.of();

        List<EducationDto> education = parsed
                ? educationRepository.findByResumeId(resumeId).stream()
                        .map(ResumeService::toEducationDto).toList()
                : List.of();

        List<ProjectDto> projects = parsed
                ? projectRepository.findByResumeId(resumeId).stream()
                        .map(ResumeService::toProjectDto).toList()
                : List.of();

        List<CertificationDto> certifications = parsed
                ? certificationRepository.findByResumeId(resumeId).stream()
                        .map(ResumeService::toCertificationDto).toList()
                : List.of();

        return new ResumeDetailResponse(
                resume.getId(),
                resume.getCandidateId(),
                resume.getFileName(),
                resume.getMimeType(),
                resume.getParseStatus(),
                resume.getEmbeddingRef(),
                candidate,
                skills,
                experiences,
                education,
                projects,
                certifications);
    }

    private static ExperienceDto toExperienceDto(Experience e) {
        return new ExperienceDto(e.getCompany(), e.getTitle(),
                e.getStartDate(), e.getEndDate(), e.getDescription());
    }

    private static EducationDto toEducationDto(Education e) {
        return new EducationDto(e.getInstitution(), e.getDegree(), e.getField(),
                e.getLevel(), e.getStartYear(), e.getEndYear());
    }

    private static ProjectDto toProjectDto(Project p) {
        return new ProjectDto(p.getName(), p.getDescription(), p.getTechStack());
    }

    private static CertificationDto toCertificationDto(Certification c) {
        return new CertificationDto(c.getName(), c.getIssuer(), c.getIssuedAt());
    }
}
