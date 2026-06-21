package com.matchly.application.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Pipeline stages (DATA_MODELS §4) and the legal transitions between them.
 *
 * <p>The happy path advances APPLIED → SCREENING → SHORTLISTED → INTERVIEW →
 * OFFER. A candidate may be REJECTED from any non-terminal stage. Terminal
 * stages (OFFER, REJECTED) accept no further moves.
 */
public enum Stage {
    APPLIED,
    SCREENING,
    SHORTLISTED,
    INTERVIEW,
    OFFER,
    REJECTED;

    private static final Map<Stage, Set<Stage>> ALLOWED = Map.of(
            APPLIED, EnumSet.of(SCREENING, REJECTED),
            SCREENING, EnumSet.of(SHORTLISTED, REJECTED),
            SHORTLISTED, EnumSet.of(INTERVIEW, REJECTED),
            INTERVIEW, EnumSet.of(OFFER, REJECTED),
            OFFER, EnumSet.noneOf(Stage.class),
            REJECTED, EnumSet.noneOf(Stage.class)
    );

    /** @return true if moving from {@code this} stage to {@code target} is permitted. */
    public boolean canTransitionTo(Stage target) {
        return ALLOWED.getOrDefault(this, EnumSet.noneOf(Stage.class)).contains(target);
    }

    /** Parse a stage name, throwing {@link IllegalArgumentException} on an unknown value. */
    public static Stage fromString(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("stage must not be null");
        }
        try {
            return Stage.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown stage: " + raw);
        }
    }
}
