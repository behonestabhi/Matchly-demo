package com.matchly.matching.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Response view of a candidate↔job skill gap, with an optional learning roadmap. */
public record SkillGapView(
        UUID candidateId,
        UUID jobId,
        List<String> missingSkills,
        JsonNode roadmap,
        Instant generatedAt
) {
}
