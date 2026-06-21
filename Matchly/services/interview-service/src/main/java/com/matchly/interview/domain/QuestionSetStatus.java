package com.matchly.interview.domain;

/** Lifecycle of a generated question set (DATA_MODELS §6). */
public enum QuestionSetStatus {
    GENERATING,
    READY,
    FAILED
}
