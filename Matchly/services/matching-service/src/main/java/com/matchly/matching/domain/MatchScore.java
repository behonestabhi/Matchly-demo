package com.matchly.matching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A persisted explainable match score. Maps DATA_MODELS §5 {@code match_scores}. */
@Entity
@Table(name = "match_scores")
public class MatchScore {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "final_score", nullable = false, precision = 4, scale = 3)
    private BigDecimal finalScore;

    @Column(name = "semantic_score", precision = 4, scale = 3)
    private BigDecimal semanticScore;

    @Column(name = "skill_overlap", precision = 4, scale = 3)
    private BigDecimal skillOverlap;

    @Column(name = "experience_fit", precision = 4, scale = 3)
    private BigDecimal experienceFit;

    @Column(name = "education_fit", precision = 4, scale = 3)
    private BigDecimal educationFit;

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @Column(name = "scored_at", nullable = false)
    private Instant scoredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MatchScore() {
        // for JPA
    }

    public MatchScore(UUID id, UUID candidateId, UUID jobId, BigDecimal finalScore,
                      BigDecimal semanticScore, BigDecimal skillOverlap, BigDecimal experienceFit,
                      BigDecimal educationFit, String modelVersion, Instant scoredAt) {
        this.id = id;
        this.candidateId = candidateId;
        this.jobId = jobId;
        this.finalScore = finalScore;
        this.semanticScore = semanticScore;
        this.skillOverlap = skillOverlap;
        this.experienceFit = experienceFit;
        this.educationFit = educationFit;
        this.modelVersion = modelVersion;
        this.scoredAt = scoredAt;
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

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

    public BigDecimal getSemanticScore() {
        return semanticScore;
    }

    public void setSemanticScore(BigDecimal semanticScore) {
        this.semanticScore = semanticScore;
    }

    public BigDecimal getSkillOverlap() {
        return skillOverlap;
    }

    public void setSkillOverlap(BigDecimal skillOverlap) {
        this.skillOverlap = skillOverlap;
    }

    public BigDecimal getExperienceFit() {
        return experienceFit;
    }

    public void setExperienceFit(BigDecimal experienceFit) {
        this.experienceFit = experienceFit;
    }

    public BigDecimal getEducationFit() {
        return educationFit;
    }

    public void setEducationFit(BigDecimal educationFit) {
        this.educationFit = educationFit;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public Instant getScoredAt() {
        return scoredAt;
    }

    public void setScoredAt(Instant scoredAt) {
        this.scoredAt = scoredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
