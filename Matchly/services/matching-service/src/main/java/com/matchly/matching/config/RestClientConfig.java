package com.matchly.matching.config;

import java.time.Duration;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Builds {@link RestClient} instances for the three downstream services with
 * bounded connect/read timeouts (so a slow or down peer cannot hang a request
 * thread). Each client carries its base URL; callers append paths.
 */
@Configuration
public class RestClientConfig {

    private final MatchingProperties props;

    public RestClientConfig(MatchingProperties props) {
        this.props = props;
    }

    private ClientHttpRequestFactory timedRequestFactory() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(props.getHttp().getConnectTimeoutMs()))
                .withReadTimeout(Duration.ofMillis(props.getHttp().getReadTimeoutMs()));
        return ClientHttpRequestFactories.get(settings);
    }

    @Bean(name = "candidateRestClient")
    public RestClient candidateRestClient() {
        return RestClient.builder()
                .baseUrl(props.getServices().getCandidateUrl())
                .requestFactory(timedRequestFactory())
                .build();
    }

    @Bean(name = "jobRestClient")
    public RestClient jobRestClient() {
        return RestClient.builder()
                .baseUrl(props.getServices().getJobUrl())
                .requestFactory(timedRequestFactory())
                .build();
    }

    @Bean(name = "aiRestClient")
    public RestClient aiRestClient() {
        return RestClient.builder()
                .baseUrl(props.getServices().getAiUrl())
                .requestFactory(timedRequestFactory())
                .build();
    }
}
