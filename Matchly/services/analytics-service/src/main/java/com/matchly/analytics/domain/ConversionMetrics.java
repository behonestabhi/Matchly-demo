package com.matchly.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Per (job, day) conversion rates (DATA_MODELS §7 {@code conversion_metrics}). */
@Entity
@Table(name = "conversion_metrics")
@IdClass(ConversionMetricsId.class)
public class ConversionMetrics {

    @Id
    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Id
    @Column(name = "day", nullable = false)
    private LocalDate day;

    @Column(name = "applications", nullable = false)
    private int applications = 0;

    @Column(name = "shortlist_rate", nullable = false)
    private BigDecimal shortlistRate = BigDecimal.ZERO;

    @Column(name = "offer_rate", nullable = false)
    private BigDecimal offerRate = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ConversionMetrics() {
    }

    public ConversionMetrics(UUID jobId, LocalDate day) {
        this.jobId = jobId;
        this.day = day;
    }

    public UUID getJobId() {
        return jobId;
    }

    public LocalDate getDay() {
        return day;
    }

    public int getApplications() {
        return applications;
    }

    public void setApplications(int applications) {
        this.applications = applications;
    }

    public BigDecimal getShortlistRate() {
        return shortlistRate;
    }

    public void setShortlistRate(BigDecimal shortlistRate) {
        this.shortlistRate = shortlistRate;
    }

    public BigDecimal getOfferRate() {
        return offerRate;
    }

    public void setOfferRate(BigDecimal offerRate) {
        this.offerRate = offerRate;
    }
}
