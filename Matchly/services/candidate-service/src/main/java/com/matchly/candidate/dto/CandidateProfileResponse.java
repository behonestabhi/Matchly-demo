package com.matchly.candidate.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchly.candidate.domain.Candidate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Candidate profile view returned by {@code GET /candidates/me} and {@code /candidates/{id}}. */
public record CandidateProfileResponse(
        UUID id,
        UUID userId,
        String headline,
        String location,
        String phone,
        JsonNode links) {

    private static final Logger log = LoggerFactory.getLogger(CandidateProfileResponse.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static CandidateProfileResponse from(Candidate c) {
        JsonNode links = null;
        if (c.getLinks() != null && !c.getLinks().isBlank()) {
            try {
                links = MAPPER.readTree(c.getLinks());
            } catch (Exception e) {
                log.warn("Could not parse stored links JSON for candidate {}", c.getId());
            }
        }
        return new CandidateProfileResponse(
                c.getId(), c.getUserId(), c.getHeadline(), c.getLocation(), c.getPhone(), links);
    }
}
