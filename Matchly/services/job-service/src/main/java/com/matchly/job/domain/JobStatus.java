package com.matchly.job.domain;

/** Lifecycle of a job posting: {@code DRAFT → OPEN → CLOSED}. */
public enum JobStatus {
    DRAFT,
    OPEN,
    CLOSED
}
