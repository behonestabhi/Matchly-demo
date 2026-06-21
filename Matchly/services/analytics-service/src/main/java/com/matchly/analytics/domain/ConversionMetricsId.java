package com.matchly.analytics.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Composite key for {@link ConversionMetrics}: (job_id, day). */
public class ConversionMetricsId implements Serializable {

    private UUID jobId;
    private LocalDate day;

    public ConversionMetricsId() {
    }

    public ConversionMetricsId(UUID jobId, LocalDate day) {
        this.jobId = jobId;
        this.day = day;
    }

    public UUID getJobId() {
        return jobId;
    }

    public LocalDate getDay() {
        return day;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConversionMetricsId that)) {
            return false;
        }
        return Objects.equals(jobId, that.jobId) && Objects.equals(day, that.day);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, day);
    }
}
