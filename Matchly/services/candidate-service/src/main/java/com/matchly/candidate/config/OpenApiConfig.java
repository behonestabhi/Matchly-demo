package com.matchly.candidate.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger / OpenAPI metadata. UI served at {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI candidateServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Matchly Candidate Service API")
                        .description("Candidate profiles, resume upload + async parsing, and skill-gap proxy.")
                        .version("v1")
                        .license(new License().name("Proprietary")));
    }
}
