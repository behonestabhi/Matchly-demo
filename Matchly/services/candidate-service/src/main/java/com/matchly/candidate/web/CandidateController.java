package com.matchly.candidate.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.matchly.candidate.client.MatchingClient;
import com.matchly.candidate.domain.Candidate;
import com.matchly.candidate.dto.CandidateProfileResponse;
import com.matchly.candidate.dto.ResumeUploadResponse;
import com.matchly.candidate.dto.UpdateProfileRequest;
import com.matchly.candidate.security.CurrentUserResolver;
import com.matchly.candidate.service.CandidateService;
import com.matchly.candidate.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Candidate Service HTTP API. Paths under {@code /api/v1/candidates}. */
@RestController
@RequestMapping("/api/v1/candidates")
@Tag(name = "Candidates", description = "Profiles, resume upload + async parsing, skill-gap proxy")
public class CandidateController {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final CandidateService candidateService;
    private final ResumeService resumeService;
    private final MatchingClient matchingClient;
    private final CurrentUserResolver currentUser;

    public CandidateController(CandidateService candidateService,
                              ResumeService resumeService,
                              MatchingClient matchingClient,
                              CurrentUserResolver currentUser) {
        this.candidateService = candidateService;
        this.resumeService = resumeService;
        this.matchingClient = matchingClient;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Get my candidate profile (provisioned on first access)")
    @GetMapping("/me")
    public CandidateProfileResponse getMe(HttpServletRequest request) {
        UUID userId = currentUser.requireUserId(request);
        Candidate candidate = candidateService.getOrCreateByUserId(userId);
        return CandidateProfileResponse.from(candidate);
    }

    @Operation(summary = "Update my candidate profile (headline, location, phone, links)")
    @PutMapping("/me")
    public CandidateProfileResponse updateMe(@Valid @RequestBody UpdateProfileRequest body,
                                             HttpServletRequest request) {
        UUID userId = currentUser.requireUserId(request);
        Candidate candidate = candidateService.updateProfile(userId, body);
        return CandidateProfileResponse.from(candidate);
    }

    @Operation(summary = "View a candidate by id (recruiter / hiring-manager)")
    @GetMapping("/{id}")
    public CandidateProfileResponse getById(@PathVariable("id") UUID id) {
        return CandidateProfileResponse.from(candidateService.getById(id));
    }

    @Operation(summary = "Upload my resume (multipart). Returns 202; parsing is async.")
    @PostMapping(path = "/me/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeUploadResponse> uploadResume(
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {
        UUID userId = currentUser.requireUserId(request);
        Candidate candidate = candidateService.getOrCreateByUserId(userId);
        String correlationId = request.getHeader(CORRELATION_HEADER);
        UUID resumeId = resumeService.upload(candidate.getId(), file, correlationId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ResumeUploadResponse.pending(resumeId));
    }

    @Operation(summary = "Skill-gap for a job (proxied to matching-service)")
    @GetMapping("/me/skill-gap")
    public JsonNode skillGap(@RequestParam("jobId") UUID jobId, HttpServletRequest request) {
        UUID userId = currentUser.requireUserId(request);
        Candidate candidate = candidateService.requireByUserId(userId);
        String correlationId = request.getHeader(CORRELATION_HEADER);
        return matchingClient.skillGap(candidate.getId(), jobId, correlationId);
    }
}
