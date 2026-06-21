package com.matchly.gateway.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/** Strongly-typed settings bound from the {@code gateway.*} block. */
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    @NestedConfigurationProperty
    private Jwt jwt = new Jwt();

    private List<String> publicPaths = List.of("/api/v1/auth/", "/actuator/", "/swagger-ui", "/v3/api-docs");

    @NestedConfigurationProperty
    private RateLimit rateLimit = new RateLimit();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    /** Returns true if the request path is public (no JWT required). */
    public boolean isPublic(String path) {
        for (String prefix : publicPaths) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static class Jwt {
        private String secret = "change-me-to-a-long-random-secret-at-least-32-chars";
        private String issuer = "matchly-auth";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }

    public static class RateLimit {
        private boolean enabled = false;
        private int replenishRate = 10;
        private int burstCapacity = 20;
        private int requestedTokens = 1;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getReplenishRate() {
            return replenishRate;
        }

        public void setReplenishRate(int replenishRate) {
            this.replenishRate = replenishRate;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }

        public int getRequestedTokens() {
            return requestedTokens;
        }

        public void setRequestedTokens(int requestedTokens) {
            this.requestedTokens = requestedTokens;
        }
    }
}
