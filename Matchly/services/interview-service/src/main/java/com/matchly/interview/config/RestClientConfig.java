package com.matchly.interview.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * {@link RestClient} beans for outbound calls. Each is configured with explicit
 * connect/read timeouts so a slow AI/LLM call cannot pin a worker thread
 * indefinitely.
 */
@Configuration
public class RestClientConfig {

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Value("${ai.service.connect-timeout-ms:3000}")
    private long aiConnectTimeoutMs;

    @Value("${ai.service.read-timeout-ms:30000}")
    private long aiReadTimeoutMs;

    @Value("${candidate.service.url:}")
    private String candidateServiceUrl;

    @Value("${job.service.url:}")
    private String jobServiceUrl;

    /** RestClient targeting the AI service for interview generation. */
    @Bean(name = "aiRestClient")
    public RestClient aiRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl(aiServiceUrl)
                .requestFactory(requestFactory(aiConnectTimeoutMs, aiReadTimeoutMs))
                .build();
    }

    /**
     * Generic RestClient for optional candidate/job lookups. Base URL is set
     * per-request by the caller (URLs may be blank/disabled).
     */
    @Bean(name = "lookupRestClient")
    public RestClient lookupRestClient(RestClient.Builder builder) {
        return builder
                .requestFactory(requestFactory(3000, 5000))
                .build();
    }

    private ClientHttpRequestFactory requestFactory(long connectMs, long readMs) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(connectMs))
                .withReadTimeout(Duration.ofMillis(readMs));
        return ClientHttpRequestFactories.get(settings);
    }
}
