package com.matchly.interview.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger / OpenAPI metadata. UI served at {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI interviewServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Matchly Interview Service API")
                        .description("Generates categorized interview questions for a candidate/job pair via the AI service.")
                        .version("v1")
                        .license(new License().name("Proprietary")));
    }
}
