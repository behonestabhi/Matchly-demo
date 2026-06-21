package com.matchly.analytics.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger / OpenAPI metadata. UI served at {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI analyticsServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Matchly Analytics Service API")
                        .description("Event-sourced recruitment analytics: funnel, time-to-hire, conversion, skill demand, and exports.")
                        .version("v1")
                        .license(new License().name("Proprietary")));
    }
}
