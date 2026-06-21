package com.matchly.application.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/** A single move through the pipeline. Maps DATA_MODELS §4 {@code stage_transitions}. */
@Entity
@Table(name = "stage_transitions")
public class StageTransition {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "application_id", nullable = false, updatable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_stage")
    private Stage fromStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_stage", nullable = false)
    private Stage toStage;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StageTransition() {
        // for JPA
    }

    public StageTransition(UUID id, UUID applicationId, Stage fromStage, Stage toStage,
                           UUID actorId, String reason, Instant occurredAt) {
        this.id = id;
        this.applicationId = applicationId;
        this.fromStage = fromStage;
        this.toStage = toStage;
        this.actorId = actorId;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public Stage getFromStage() {
        return fromStage;
    }

    public Stage getToStage() {
        return toStage;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
