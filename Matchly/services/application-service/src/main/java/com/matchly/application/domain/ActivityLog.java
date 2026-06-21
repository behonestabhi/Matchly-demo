package com.matchly.application.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Append-only audit record. Maps DATA_MODELS §4 {@code activity_log}.
 *
 * <p>{@code payload} is stored as a JSONB column; we keep it as a JSON
 * {@code String} here (the service serializes a map to JSON before persisting)
 * which keeps the entity free of a JSON object-mapping dependency.
 */
@Entity
@Table(name = "activity_log")
public class ActivityLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "action", nullable = false)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected ActivityLog() {
        // for JPA
    }

    public ActivityLog(UUID id, UUID applicationId, UUID actorId, String action,
                       String payload, Instant occurredAt) {
        this.id = id;
        this.applicationId = applicationId;
        this.actorId = actorId;
        this.action = action;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
