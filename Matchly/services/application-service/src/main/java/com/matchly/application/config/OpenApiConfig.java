package com.matchly.application.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger / OpenAPI metadata. UI served at {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI applicationServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Matchly Application / ATS Service API")
                        .description("Applications, pipeline stages, recruiter comments and audit trail.")
                        .version("v1")
                        .license(new License().name("Proprietary")));
    }
}
