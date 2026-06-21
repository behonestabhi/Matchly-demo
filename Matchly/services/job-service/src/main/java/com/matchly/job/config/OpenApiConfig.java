package com.matchly.job.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger / OpenAPI metadata. UI served at {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jobServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Matchly Job Service API")
                        .description("Job postings, lifecycle (DRAFT → OPEN → CLOSED), search, and JD embedding.")
                        .version("v1")
                        .license(new License().name("Proprietary")));
    }
}
