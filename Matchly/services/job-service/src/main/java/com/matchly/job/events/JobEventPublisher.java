package com.matchly.job.events;

import com.matchly.job.domain.Job;
import com.matchly.job.domain.JobRequiredSkill;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Produces job-domain events onto {@code job.events} (keyed by jobId, so a job's
 * events keep per-entity ordering), wrapped in the common {@link EventEnvelope}.
 * Consumed by the matching and analytics services (see DATA_MODELS §9).
 */
@Component
public class JobEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(JobEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String jobEventsTopic;

    public JobEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.topics.job-events:job.events}") String jobEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.jobEventsTopic = jobEventsTopic;
    }

    /** {@code JobPosted} → {@code job.events} (key = jobId). */
    public void publishJobPosted(Job job, List<JobRequiredSkill> skills, String correlationId) {
        List<RequiredSkill> skillPayload = skills.stream()
                .map(s -> new RequiredSkill(s.getSkillName(), s.getWeight(), s.getRequired()))
                .toList();
        JobPosted payload = new JobPosted(
                job.getId().toString(),
                job.getRecruiterId().toString(),
                skillPayload,
                job.getMinExpYrs(),
                job.getMaxExpYrs());
        send(job.getId().toString(), EventEnvelope.of("JobPosted", correlationId, payload));
    }

    /** {@code JobClosed} → {@code job.events} (key = jobId). */
    public void publishJobClosed(UUID jobId, String correlationId) {
        send(jobId.toString(),
                EventEnvelope.of("JobClosed", correlationId, new JobClosed(jobId.toString())));
    }

    private void send(String key, EventEnvelope envelope) {
        try {
            kafkaTemplate.send(jobEventsTopic, key, envelope)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} to {} (key={})",
                                    envelope.eventType(), jobEventsTopic, key, ex);
                        } else {
                            log.debug("Published {} to {} (key={})",
                                    envelope.eventType(), jobEventsTopic, key);
                        }
                    });
        } catch (Exception ex) {
            // Producing must never break the calling flow.
            log.error("Error sending {} to {} (key={})", envelope.eventType(), jobEventsTopic, key, ex);
        }
    }

    /** Payload for {@code JobPosted} (events.md). */
    public record JobPosted(String jobId, String recruiterId, List<RequiredSkill> requiredSkills,
                            Double minExpYrs, Double maxExpYrs) {
    }

    /** Payload for {@code JobClosed} (events.md). */
    public record JobClosed(String jobId) {
    }

    /** A required-skill entry inside {@code JobPosted.requiredSkills}. */
    public record RequiredSkill(String skillName, Double weight, Boolean required) {
    }
}
