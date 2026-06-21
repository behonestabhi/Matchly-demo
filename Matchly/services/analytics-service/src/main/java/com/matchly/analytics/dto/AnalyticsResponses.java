package com.matchly.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Response shapes for the analytics read API (API_CONTRACTS — Analytics Service). */
public final class AnalyticsResponses {

    private AnalyticsResponses() {
    }

    /** {@code GET /analytics/overview}. */
    public record Overview(
            long applicationsReceived,
            long shortlisted,
            long hires,
            Double avgTimeToHireDays) {
    }

    /** Aggregate funnel totals for a job, plus a per-day breakdown. */
    public record Funnel(
            UUID jobId,
            FunnelCounts totals,
            List<FunnelDay> daily) {
    }

    public record FunnelCounts(
            int applied, int screening, int shortlisted,
            int interview, int offer, int rejected) {
    }

    public record FunnelDay(LocalDate day, FunnelCounts counts) {
    }

    /** {@code GET /analytics/time-to-hire}. */
    public record TimeToHire(
            long hires,
            Double avgDays,
            List<HirePoint> points) {
    }

    public record HirePoint(UUID applicationId, UUID jobId, Instant hiredAt, BigDecimal daysToHire) {
    }

    /** {@code GET /analytics/conversion}. */
    public record Conversion(
            UUID jobId,
            long totalApplications,
            BigDecimal avgShortlistRate,
            BigDecimal avgOfferRate,
            List<ConversionDay> daily) {
    }

    public record ConversionDay(
            LocalDate day, int applications, BigDecimal shortlistRate, BigDecimal offerRate) {
    }

    /** {@code GET /analytics/skills/in-demand}. */
    public record SkillDemandItem(String skillName, long jobCount) {
    }
}
