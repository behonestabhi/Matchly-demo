package com.matchly.application.dto;

import com.matchly.application.domain.Comment;
import java.time.Instant;
import java.util.UUID;

/** Response shape for a recruiter comment. */
public record CommentResponse(
        UUID id,
        UUID applicationId,
        UUID authorId,
        String body,
        String visibility,
        Instant createdAt) {

    public static CommentResponse from(Comment c) {
        return new CommentResponse(
                c.getId(),
                c.getApplicationId(),
                c.getAuthorId(),
                c.getBody(),
                c.getVisibility(),
                c.getCreatedAt());
    }
}
