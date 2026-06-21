package com.matchly.matching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Matchly Matching Service.
 *
 * <p>Orchestrates candidate↔job scoring: consumes {@code ApplicationSubmitted}
 * from {@code application.events}, fetches candidate/job data from their services,
 * calls the AI service to score, persists {@code match_scores} / {@code skill_gaps},
 * and produces {@code MatchScored} to {@code matching.events}. Also serves ranked
 * reads (candidates-for-a-job, jobs-for-a-candidate, single score, skill-gap,
 * success prediction).
 */
@SpringBootApplication
public class MatchingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchingServiceApplication.class, args);
    }
}
