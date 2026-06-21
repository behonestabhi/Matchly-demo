package com.matchly.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point for the Matchly Job Service.
 *
 * <p>Owns job postings and their lifecycle (DRAFT → OPEN → CLOSED). On publish it
 * embeds the JD via the AI service and produces {@code JobPosted}; on close it
 * produces {@code JobClosed}.
 */
@SpringBootApplication
@EnableJpaAuditing
public class JobServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobServiceApplication.class, args);
    }
}
