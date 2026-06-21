package com.matchly.candidate.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code PUT /candidates/me}. All fields optional; {@code links} is a
 * free-form JSON object such as {@code {github, linkedin, portfolio}}.
 */
public record UpdateProfileRequest(
        @Size(max = 280) String headline,
        @Size(max = 280) String location,
        @Size(max = 64) String phone,
        JsonNode links) {
}
