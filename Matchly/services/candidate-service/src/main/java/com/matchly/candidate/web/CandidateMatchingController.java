package com.matchly.candidate.web;

import com.matchly.candidate.dto.CandidateMatchingProfileResponse;
import com.matchly.candidate.service.CandidateMatchingProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * In-cluster-only endpoint for the matching service. Lives under {@code /internal}
 * so the API gateway (which routes {@code /api/v1/candidates/**}) does not expose
 * it externally — it is reachable only by service-to-service calls inside the
 * cluster network.
 */
@RestController
@RequestMapping("/internal/candidates")
@Tag(name = "Internal", description = "Service-to-service endpoints (not gateway-exposed)")
public class CandidateMatchingController {

    private final CandidateMatchingProfileService profileService;

    public CandidateMatchingController(CandidateMatchingProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "Matching profile for a candidate (skills, experience, embedding ref)")
    @GetMapping("/{id}/matching-profile")
    public CandidateMatchingProfileResponse matchingProfile(@PathVariable("id") UUID id) {
        return profileService.assemble(id);
    }
}
