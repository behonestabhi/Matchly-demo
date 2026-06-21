package com.matchly.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Per (job, day) stage counts (DATA_MODELS §7 {@code funnel_daily}). */
@Entity
@Table(name = "funnel_daily")
@IdClass(FunnelDailyId.class)
public class FunnelDaily {

    @Id
    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Id
    @Column(name = "day", nullable = false)
    private LocalDate day;

    @Column(name = "applied", nullable = false)
    private int applied = 0;

    @Column(name = "screening", nullable = false)
    private int screening = 0;

    @Column(name = "shortlisted", nullable = false)
    private int shortlisted = 0;

    @Column(name = "interview", nullable = false)
    private int interview = 0;

    @Column(name = "offer", nullable = false)
    private int offer = 0;

    @Column(name = "rejected", nullable = false)
    private int rejected = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FunnelDaily() {
    }

    public FunnelDaily(UUID jobId, LocalDate day) {
        this.jobId = jobId;
        this.day = day;
    }

    /** Increment the counter for a given stage name (case-insensitive). */
    public void incrementStage(String stage) {
        if (stage == null) {
            return;
        }
        switch (stage.trim().toUpperCase()) {
            case "APPLIED" -> applied++;
            case "SCREENING" -> screening++;
            case "SHORTLISTED" -> shortlisted++;
            case "INTERVIEW" -> interview++;
            case "OFFER" -> offer++;
            case "REJECTED" -> rejected++;
            default -> { /* unknown stage: ignore */ }
        }
    }

    public UUID getJobId() {
        return jobId;
    }

    public LocalDate getDay() {
        return day;
    }

    public int getApplied() {
        return applied;
    }

    public int getScreening() {
        return screening;
    }

    public int getShortlisted() {
        return shortlisted;
    }

    public int getInterview() {
        return interview;
    }

    public int getOffer() {
        return offer;
    }

    public int getRejected() {
        return rejected;
    }
}
