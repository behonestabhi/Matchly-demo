package com.matchly.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Matchly Application / ATS Service.
 *
 * <p>Owns applications (candidate ↔ job join), pipeline stages, recruiter
 * comments and an append-only activity log. Produces {@code ApplicationSubmitted}
 * and {@code StageChanged} domain events to the {@code application.events} topic.
 */
@SpringBootApplication
public class ApplicationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationServiceApplication.class, args);
    }
}
