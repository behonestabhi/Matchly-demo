package com.matchly.candidate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the Matchly Candidate Service.
 *
 * <p>Owns candidate profiles and the <em>structured</em> parsed resume. Resume
 * parsing is orchestrated over HTTP against the AI service asynchronously; once
 * complete it produces {@code ResumeParsed} / {@code ResumeUploaded} events.
 */
@SpringBootApplication
@EnableAsync
@EnableJpaAuditing
public class CandidateServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CandidateServiceApplication.class, args);
    }
}
