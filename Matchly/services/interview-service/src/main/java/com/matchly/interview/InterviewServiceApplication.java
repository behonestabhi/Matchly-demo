package com.matchly.interview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the Matchly Interview Service.
 *
 * <p>Generates categorized interview questions for a (candidate, job) pair by
 * delegating to the AI service, persisting the result, and emitting a
 * {@code QuestionsGenerated} event on {@code interview.events}.
 */
@SpringBootApplication
@EnableAsync
public class InterviewServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewServiceApplication.class, args);
    }
}
