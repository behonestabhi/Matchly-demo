package com.matchly.analytics.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Composite key for {@link FunnelDaily}: (job_id, day). */
public class FunnelDailyId implements Serializable {

    private UUID jobId;
    private LocalDate day;

    public FunnelDailyId() {
    }

    public FunnelDailyId(UUID jobId, LocalDate day) {
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
        if (!(o instanceof FunnelDailyId that)) {
            return false;
        }
        return Objects.equals(jobId, that.jobId) && Objects.equals(day, that.day);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, day);
    }
}
