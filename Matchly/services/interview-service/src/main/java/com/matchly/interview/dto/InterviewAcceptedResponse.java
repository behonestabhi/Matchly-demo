package com.matchly.interview.dto;

import java.util.UUID;

/** 202 response for {@code POST /api/v1/interviews}. */
public record InterviewAcceptedResponse(UUID id, String status, String poll) {

    public static InterviewAcceptedResponse of(UUID id, String status) {
        return new InterviewAcceptedResponse(id, status, "/api/v1/interviews/" + id);
    }
}
