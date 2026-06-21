package com.matchly.candidate.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executor backing {@code @Async} resume-parse orchestration. Resume upload
 * returns 202 immediately while parsing (AI parse + embed + persistence + event
 * production) runs on this pool.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "resumeParseExecutor")
    public Executor resumeParseExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("resume-parse-");
        executor.initialize();
        return executor;
    }
}
