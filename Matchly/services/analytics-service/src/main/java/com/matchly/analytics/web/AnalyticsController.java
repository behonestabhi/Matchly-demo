package com.matchly.analytics.web;

import com.matchly.analytics.dto.AnalyticsResponses.Conversion;
import com.matchly.analytics.dto.AnalyticsResponses.Funnel;
import com.matchly.analytics.dto.AnalyticsResponses.Overview;
import com.matchly.analytics.dto.AnalyticsResponses.SkillDemandItem;
import com.matchly.analytics.dto.AnalyticsResponses.TimeToHire;
import com.matchly.analytics.exception.BadRequestException;
import com.matchly.analytics.service.AnalyticsQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Recruiter analytics dashboards (API_CONTRACTS — Analytics Service). */
@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Funnel, time-to-hire, conversion, in-demand skills")
public class AnalyticsController {

    private final AnalyticsQueryService queryService;

    public AnalyticsController(AnalyticsQueryService queryService) {
        this.queryService = queryService;
    }

    @Operation(summary = "Headline metrics: applications, shortlisted, hires, avg time-to-hire")
    @GetMapping("/overview")
    public Overview overview() {
        return queryService.overview();
    }

    @Operation(summary = "Stage-by-stage funnel for a job")
    @GetMapping("/funnel")
    public Funnel funnel(@RequestParam UUID jobId) {
        return queryService.funnel(jobId);
    }

    @Operation(summary = "Time-to-hire (optionally bounded by an ISO-8601 from/to window)")
    @GetMapping("/time-to-hire")
    public TimeToHire timeToHire(@RequestParam(required = false) String from,
                                 @RequestParam(required = false) String to) {
        return queryService.timeToHire(parseInstant(from, "from"), parseInstant(to, "to"));
    }

    @Operation(summary = "Conversion / shortlist / offer rates for a job")
    @GetMapping("/conversion")
    public Conversion conversion(@RequestParam UUID jobId) {
        return queryService.conversion(jobId);
    }

    @Operation(summary = "Most in-demand skills within a window (e.g. 30d, 7d)")
    @GetMapping("/skills/in-demand")
    public List<SkillDemandItem> inDemand(@RequestParam(defaultValue = "30d") String window) {
        LocalDate since = LocalDate.now(ZoneOffset.UTC).minusDays(parseWindowDays(window));
        return queryService.inDemandSkills(since);
    }

    @Operation(summary = "Export a report as CSV (type = overview | skills)")
    @GetMapping("/reports/export")
    public ResponseEntity<String> export(@RequestParam(defaultValue = "overview") String type) {
        String csv;
        String filename;
        switch (type.toLowerCase()) {
            case "overview" -> {
                Overview o = queryService.overview();
                csv = "metric,value\n"
                        + "applicationsReceived," + o.applicationsReceived() + "\n"
                        + "shortlisted," + o.shortlisted() + "\n"
                        + "hires," + o.hires() + "\n"
                        + "avgTimeToHireDays," + (o.avgTimeToHireDays() == null ? "" : o.avgTimeToHireDays()) + "\n";
                filename = "overview.csv";
            }
            case "skills" -> {
                LocalDate since = LocalDate.now(ZoneOffset.UTC).minusDays(30);
                StringBuilder sb = new StringBuilder("skillName,jobCount\n");
                for (SkillDemandItem s : queryService.inDemandSkills(since)) {
                    sb.append(escape(s.skillName())).append(',').append(s.jobCount()).append('\n');
                }
                csv = sb.toString();
                filename = "skills-in-demand.csv";
            }
            default -> throw new BadRequestException("Unknown report type: " + type + " (expected overview|skills)");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    private Instant parseInstant(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid " + field + " (expected ISO-8601 instant): " + value);
        }
    }

    private long parseWindowDays(String window) {
        String w = window == null ? "30d" : window.trim().toLowerCase();
        if (w.endsWith("d")) {
            w = w.substring(0, w.length() - 1);
        }
        try {
            long days = Long.parseLong(w);
            return days > 0 ? days : 30;
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid window (expected e.g. 30d): " + window);
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
