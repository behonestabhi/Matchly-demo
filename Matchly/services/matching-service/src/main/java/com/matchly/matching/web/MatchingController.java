package com.matchly.matching.web;

import com.matchly.matching.dto.MatchScoreView;
import com.matchly.matching.dto.PageResponse;
import com.matchly.matching.dto.PredictionView;
import com.matchly.matching.dto.SkillGapView;
import com.matchly.matching.service.MatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API for matching (API_CONTRACTS — Matching Service). Paths under {@code /api/v1/matching}. */
@RestController
@RequestMapping("/api/v1/matching")
@Tag(name = "Matching", description = "Ranked candidates/jobs, match scores, skill gaps, success prediction")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @Operation(summary = "Ranked candidates for a job (highest match first)")
    @GetMapping("/jobs/{jobId}/candidates")
    public PageResponse<MatchScoreView> candidatesForJob(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return matchingService.rankedCandidates(jobId, page, size);
    }

    @Operation(summary = "Best-matching jobs for a candidate")
    @GetMapping("/candidates/{candidateId}/jobs")
    public PageResponse<MatchScoreView> jobsForCandidate(
            @PathVariable UUID candidateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return matchingService.jobsForCandidate(candidateId, page, size);
    }

    @Operation(summary = "Single match score + breakdown (computes on demand if absent)")
    @GetMapping("/score")
    public MatchScoreView score(@RequestParam UUID candidateId, @RequestParam UUID jobId) {
        return matchingService.getScore(candidateId, jobId);
    }

    @Operation(summary = "Skill gap + learning roadmap for a candidate↔job pair")
    @GetMapping("/skill-gap")
    public SkillGapView skillGap(@RequestParam UUID candidateId, @RequestParam UUID jobId) {
        return matchingService.getSkillGap(candidateId, jobId);
    }

    @Operation(summary = "Force re-ranking of a job's known candidates")
    @PostMapping("/jobs/{jobId}/rescore")
    public ResponseEntity<Map<String, Object>> rescore(@PathVariable UUID jobId, HttpServletRequest http) {
        int count = matchingService.rescoreJob(jobId, http.getHeader("X-Correlation-Id"));
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("jobId", jobId, "rescored", count, "status", "ACCEPTED"));
    }

    @Operation(summary = "ML success probability for a candidate↔job pair")
    @GetMapping("/predict")
    public PredictionView predict(@RequestParam UUID candidateId, @RequestParam UUID jobId) {
        return matchingService.predict(candidateId, jobId);
    }
}
