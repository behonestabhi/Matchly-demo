package com.matchly.candidate.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * {@link RestClient} beans for outbound calls. The AI client is used by the
 * async resume-parse flow; the matching client backs the skill-gap proxy. Both
 * carry connect/read timeouts so a slow/down dependency cannot wedge a thread.
 */
@Configuration
public class RestClientConfig {

    @Value("${ai.service-url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${matching.service-url:http://localhost:8085}")
    private String matchingServiceUrl;

    @Value("${http-client.connect-timeout-ms:3000}")
    private long connectTimeoutMs;

    @Value("${http-client.read-timeout-ms:30000}")
    private long readTimeoutMs;

    private ClientHttpRequestFactory requestFactory() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .withReadTimeout(Duration.ofMillis(readTimeoutMs));
        return ClientHttpRequestFactories.get(settings);
    }

    /** Client for the AI service ({@code AI_SERVICE_URL}). */
    @Bean("aiRestClient")
    public RestClient aiRestClient() {
        return RestClient.builder()
                .baseUrl(aiServiceUrl)
                .requestFactory(requestFactory())
                .build();
    }

    /** Client for the Matching service ({@code MATCHING_SERVICE_URL}). */
    @Bean("matchingRestClient")
    public RestClient matchingRestClient() {
        return RestClient.builder()
                .baseUrl(matchingServiceUrl)
                .requestFactory(requestFactory())
                .build();
    }
}
