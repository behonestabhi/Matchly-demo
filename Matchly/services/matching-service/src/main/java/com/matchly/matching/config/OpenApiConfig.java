package com.matchly.matching.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger / OpenAPI metadata. UI served at {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI matchingServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Matchly Matching Service API")
                        .description("Candidate↔job match scores, rankings, skill gaps and success predictions.")
                        .version("v1")
                        .license(new License().name("Proprietary")));
    }
}
