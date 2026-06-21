package com.matchly.analytics.service;

import com.matchly.analytics.domain.ConversionMetrics;
import com.matchly.analytics.domain.FunnelDaily;
import com.matchly.analytics.dto.AnalyticsResponses.Conversion;
import com.matchly.analytics.dto.AnalyticsResponses.ConversionDay;
import com.matchly.analytics.dto.AnalyticsResponses.Funnel;
import com.matchly.analytics.dto.AnalyticsResponses.FunnelCounts;
import com.matchly.analytics.dto.AnalyticsResponses.FunnelDay;
import com.matchly.analytics.dto.AnalyticsResponses.HirePoint;
import com.matchly.analytics.dto.AnalyticsResponses.Overview;
import com.matchly.analytics.dto.AnalyticsResponses.SkillDemandItem;
import com.matchly.analytics.dto.AnalyticsResponses.TimeToHire;
import com.matchly.analytics.repository.ConversionMetricsRepository;
import com.matchly.analytics.repository.FunnelDailyRepository;
import com.matchly.analytics.repository.SkillDemandRepository;
import com.matchly.analytics.repository.TimeToHireRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only queries that assemble the analytics dashboards from the read models. */
@Service
@Transactional(readOnly = true)
public class AnalyticsQueryService {

    private final FunnelDailyRepository funnelRepository;
    private final TimeToHireRepository timeToHireRepository;
    private final SkillDemandRepository skillDemandRepository;
    private final ConversionMetricsRepository conversionRepository;

    public AnalyticsQueryService(FunnelDailyRepository funnelRepository,
                                 TimeToHireRepository timeToHireRepository,
                                 SkillDemandRepository skillDemandRepository,
                                 ConversionMetricsRepository conversionRepository) {
        this.funnelRepository = funnelRepository;
        this.timeToHireRepository = timeToHireRepository;
        this.skillDemandRepository = skillDemandRepository;
        this.conversionRepository = conversionRepository;
    }

    public Overview overview() {
        long applications = 0;
        long shortlisted = 0;
        long hires = 0;
        for (FunnelDaily f : funnelRepository.findAll()) {
            applications += f.getApplied();
            shortlisted += f.getShortlisted();
            hires += f.getOffer();
        }
        Double avgDays = averageDaysToHire(timeToHireRepository.findByHiredAtIsNotNull());
        return new Overview(applications, shortlisted, hires, avgDays);
    }

    public Funnel funnel(UUID jobId) {
        List<FunnelDaily> rows = funnelRepository.findByJobIdOrderByDayAsc(jobId);
        int applied = 0, screening = 0, shortlisted = 0, interview = 0, offer = 0, rejected = 0;
        List<FunnelDay> daily = new java.util.ArrayList<>();
        for (FunnelDaily f : rows) {
            applied += f.getApplied();
            screening += f.getScreening();
            shortlisted += f.getShortlisted();
            interview += f.getInterview();
            offer += f.getOffer();
            rejected += f.getRejected();
            daily.add(new FunnelDay(f.getDay(), new FunnelCounts(
                    f.getApplied(), f.getScreening(), f.getShortlisted(),
                    f.getInterview(), f.getOffer(), f.getRejected())));
        }
        return new Funnel(jobId,
                new FunnelCounts(applied, screening, shortlisted, interview, offer, rejected),
                daily);
    }

    public TimeToHire timeToHire(Instant from, Instant to) {
        List<com.matchly.analytics.domain.TimeToHire> rows = (from != null && to != null)
                ? timeToHireRepository.findByHiredAtBetween(from, to)
                : timeToHireRepository.findByHiredAtIsNotNull();
        List<HirePoint> points = rows.stream()
                .map(t -> new HirePoint(t.getApplicationId(), t.getJobId(), t.getHiredAt(), t.getDaysToHire()))
                .toList();
        return new TimeToHire(points.size(), averageDaysToHire(rows), points);
    }

    public Conversion conversion(UUID jobId) {
        List<ConversionMetrics> rows = conversionRepository.findByJobIdOrderByDayAsc(jobId);
        long totalApps = 0;
        BigDecimal shortlistSum = BigDecimal.ZERO;
        BigDecimal offerSum = BigDecimal.ZERO;
        List<ConversionDay> daily = new java.util.ArrayList<>();
        for (ConversionMetrics c : rows) {
            totalApps += c.getApplications();
            shortlistSum = shortlistSum.add(nz(c.getShortlistRate()));
            offerSum = offerSum.add(nz(c.getOfferRate()));
            daily.add(new ConversionDay(c.getDay(), c.getApplications(),
                    nz(c.getShortlistRate()), nz(c.getOfferRate())));
        }
        int n = rows.size();
        BigDecimal avgShortlist = n == 0 ? BigDecimal.ZERO
                : shortlistSum.divide(BigDecimal.valueOf(n), 3, RoundingMode.HALF_UP);
        BigDecimal avgOffer = n == 0 ? BigDecimal.ZERO
                : offerSum.divide(BigDecimal.valueOf(n), 3, RoundingMode.HALF_UP);
        return new Conversion(jobId, totalApps, avgShortlist, avgOffer, daily);
    }

    public List<SkillDemandItem> inDemandSkills(LocalDate since) {
        return skillDemandRepository.aggregateSince(since).stream()
                .map(sc -> new SkillDemandItem(sc.getSkillName(),
                        sc.getTotal() == null ? 0L : sc.getTotal()))
                .toList();
    }

    private Double averageDaysToHire(List<com.matchly.analytics.domain.TimeToHire> rows) {
        var stats = rows.stream()
                .map(com.matchly.analytics.domain.TimeToHire::getDaysToHire)
                .filter(d -> d != null)
                .mapToDouble(BigDecimal::doubleValue)
                .summaryStatistics();
        return stats.getCount() == 0 ? null
                : BigDecimal.valueOf(stats.getAverage()).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
