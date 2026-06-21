package com.matchly.candidate.dto;

import java.util.UUID;

/**
 * 202 response for {@code POST /candidates/me/resume}.
 *
 * <pre>{ "resumeId":"...", "parseStatus":"PENDING", "poll":"/api/v1/resumes/{resumeId}" }</pre>
 */
public record ResumeUploadResponse(UUID resumeId, String parseStatus, String poll) {

    public static ResumeUploadResponse pending(UUID resumeId) {
        return new ResumeUploadResponse(resumeId, "PENDING", "/api/v1/resumes/" + resumeId);
    }
}
