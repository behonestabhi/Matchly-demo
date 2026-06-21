package com.matchly.matching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Persisted ML success prediction. Maps DATA_MODELS §5 {@code success_predictions}. */
@Entity
@Table(name = "success_predictions")
public class SuccessPrediction {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "probability", nullable = false, precision = 4, scale = 3)
    private BigDecimal probability;

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "jsonb")
    private String features;

    @Column(name = "predicted_at", nullable = false)
    private Instant predictedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SuccessPrediction() {
        // for JPA
    }

    public SuccessPrediction(UUID id, UUID candidateId, UUID jobId, BigDecimal probability,
                             String modelVersion, String features, Instant predictedAt) {
        this.id = id;
        this.candidateId = candidateId;
        this.jobId = jobId;
        this.probability = probability;
        this.modelVersion = modelVersion;
        this.features = features;
        this.predictedAt = predictedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public BigDecimal getProbability() {
        return probability;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getFeatures() {
        return features;
    }

    public Instant getPredictedAt() {
        return predictedAt;
    }
}
