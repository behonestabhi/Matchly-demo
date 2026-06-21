package com.matchly.interview.web;

import com.matchly.interview.dto.CreateInterviewRequest;
import com.matchly.interview.dto.InterviewAcceptedResponse;
import com.matchly.interview.dto.QuestionSetView;
import com.matchly.interview.service.InterviewService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Interview question-set API (API_CONTRACTS.md "Interview Service"). All paths
 * are the full gateway path {@code /api/v1/interviews/...}.
 */
@RestController
@RequestMapping("/api/v1/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    /**
     * Kick off async question generation. Returns 202 with a pollable resource:
     * {@code {id, status:"GENERATING", poll}}.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public InterviewAcceptedResponse create(@Valid @RequestBody CreateInterviewRequest request) {
        UUID id = interviewService.create(request);
        return InterviewAcceptedResponse.of(id, "GENERATING");
    }

    /** Fetch a question set (set + questions + status). */
    @GetMapping("/{id}")
    public ResponseEntity<QuestionSetView> get(@PathVariable UUID id) {
        return ResponseEntity.ok(interviewService.get(id));
    }

    /** List existing sets, optionally filtered by candidateId and/or jobId. */
    @GetMapping
    public List<QuestionSetView> list(
            @RequestParam(required = false) UUID candidateId,
            @RequestParam(required = false) UUID jobId) {
        return interviewService.findByPair(candidateId, jobId);
    }
}
