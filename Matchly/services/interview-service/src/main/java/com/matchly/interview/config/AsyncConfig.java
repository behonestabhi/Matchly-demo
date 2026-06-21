package com.matchly.interview.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Thread pool backing {@code @Async} question generation, so the
 * {@code POST /api/v1/interviews} request can return 202 immediately while the
 * LLM call runs in the background.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "interviewGenerationExecutor")
    public Executor interviewGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("interview-gen-");
        executor.initialize();
        return executor;
    }
}
