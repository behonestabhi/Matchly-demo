package com.matchly.matching.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

/**
 * Configuration bound from {@code matchly.*}: downstream service URLs, Kafka
 * topic names, HTTP timeouts and scoring defaults. All env-overridable.
 */
@Component
@ConfigurationProperties(prefix = "matchly")
public class MatchingProperties {

    @NestedConfigurationProperty
    private Services services = new Services();

    @NestedConfigurationProperty
    private Kafka kafka = new Kafka();

    @NestedConfigurationProperty
    private Http http = new Http();

    @NestedConfigurationProperty
    private Scoring scoring = new Scoring();

    public Services getServices() {
        return services;
    }

    public void setServices(Services services) {
        this.services = services;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    public Http getHttp() {
        return http;
    }

    public void setHttp(Http http) {
        this.http = http;
    }

    public Scoring getScoring() {
        return scoring;
    }

    public void setScoring(Scoring scoring) {
        this.scoring = scoring;
    }

    public static class Services {
        private String candidateUrl = "http://localhost:8082";
        private String jobUrl = "http://localhost:8083";
        private String aiUrl = "http://localhost:8000";

        public String getCandidateUrl() {
            return candidateUrl;
        }

        public void setCandidateUrl(String candidateUrl) {
            this.candidateUrl = candidateUrl;
        }

        public String getJobUrl() {
            return jobUrl;
        }

        public void setJobUrl(String jobUrl) {
            this.jobUrl = jobUrl;
        }

        public String getAiUrl() {
            return aiUrl;
        }

        public void setAiUrl(String aiUrl) {
            this.aiUrl = aiUrl;
        }
    }

    public static class Kafka {
        @NestedConfigurationProperty
        private Topics topics = new Topics();

        public Topics getTopics() {
            return topics;
        }

        public void setTopics(Topics topics) {
            this.topics = topics;
        }

        public static class Topics {
            private String applicationEvents = "application.events";
            private String matchingEvents = "matching.events";

            public String getApplicationEvents() {
                return applicationEvents;
            }

            public void setApplicationEvents(String applicationEvents) {
                this.applicationEvents = applicationEvents;
            }

            public String getMatchingEvents() {
                return matchingEvents;
            }

            public void setMatchingEvents(String matchingEvents) {
                this.matchingEvents = matchingEvents;
            }
        }
    }

    public static class Http {
        private int connectTimeoutMs = 2000;
        private int readTimeoutMs = 5000;

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }

    public static class Scoring {
        private String modelVersion = "match-v1.0";

        public String getModelVersion() {
            return modelVersion;
        }

        public void setModelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
        }
    }
}
