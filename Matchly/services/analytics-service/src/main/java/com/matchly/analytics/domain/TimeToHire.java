package com.matchly.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** One row per application; days_to_hire filled when the OFFER stage is reached
 * (DATA_MODELS §7 {@code time_to_hire}). */
@Entity
@Table(name = "time_to_hire")
public class TimeToHire {

    @Id
    @Column(name = "application_id", nullable = false, updatable = false)
    private UUID applicationId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "hired_at")
    private Instant hiredAt;

    @Column(name = "days_to_hire")
    private BigDecimal daysToHire;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TimeToHire() {
    }

    public TimeToHire(UUID applicationId, UUID jobId, Instant appliedAt) {
        this.applicationId = applicationId;
        this.jobId = jobId;
        this.appliedAt = appliedAt;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }

    public Instant getHiredAt() {
        return hiredAt;
    }

    public void setHiredAt(Instant hiredAt) {
        this.hiredAt = hiredAt;
    }

    public BigDecimal getDaysToHire() {
        return daysToHire;
    }

    public void setDaysToHire(BigDecimal daysToHire) {
        this.daysToHire = daysToHire;
    }
}
