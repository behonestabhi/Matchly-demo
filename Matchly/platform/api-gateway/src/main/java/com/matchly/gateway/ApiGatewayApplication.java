package com.matchly.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Matchly API Gateway (Spring Cloud Gateway, reactive).
 *
 * <p>Edge for all external traffic: routes by path prefix, generates/propagates
 * a correlation id, validates the HS256 JWT on protected paths, and forwards
 * trusted identity headers ({@code X-User-Id}, {@code X-User-Roles}) downstream.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
