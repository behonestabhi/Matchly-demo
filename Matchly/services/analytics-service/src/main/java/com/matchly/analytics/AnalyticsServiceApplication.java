package com.matchly.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Matchly Analytics Service.
 *
 * <p>Consumes domain events from Kafka and maintains denormalized read-model
 * projections (funnel, time-to-hire, skill demand, conversion) that back the
 * recruiter analytics dashboards.
 */
@SpringBootApplication
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
