package com.matchly.job.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * {@link RestClient} bean for outbound calls to the AI service (JD embedding on
 * publish). Connect/read timeouts keep a slow or down AI service from wedging a
 * request thread; embedding failures are caught and the job still publishes.
 */
@Configuration
public class RestClientConfig {

    @Value("${ai.service-url:http://localhost:8000}")
    private String aiServiceUrl;

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

    @Bean("aiRestClient")
    public RestClient aiRestClient() {
        return RestClient.builder()
                .baseUrl(aiServiceUrl)
                .requestFactory(requestFactory())
                .build();
    }
}
