package com.matchly.candidate.service;

import com.matchly.candidate.client.AiClient;
import com.matchly.candidate.client.AiDtos.EmbedRequest;
import com.matchly.candidate.client.AiDtos.EmbedResponse;
import com.matchly.candidate.client.AiDtos.ParseRequest;
import com.matchly.candidate.client.AiDtos.ParseResponse;
import com.matchly.candidate.events.CandidateEventPublisher;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * HTTP-orchestrated resume parse flow (NOT a Kafka consumer). Runs asynchronously
 * after upload returns 202:
 *
 * <ol>
 *   <li>POST {@code AI_SERVICE_URL/internal/parse} with the file bytes.</li>
 *   <li>Persist structured {@code parsed_resumes} / skills / experiences / education /
 *       projects / certifications.</li>
 *   <li>POST {@code /internal/embed} for the resume text; store {@code embedding_ref}.</li>
 *   <li>Set {@code parse_status=PARSED} and produce {@code ResumeParsed} ({@code resume.parsed}).</li>
 * </ol>
 *
 * On any AI failure the resume is marked {@code FAILED} and no ResumeParsed event
 * is produced. Each persistence step is its own transaction (this bean is not the
 * transaction-managed service; it delegates to {@link ResumePersistenceService}).
 */
@Service
public class ResumeParseOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ResumeParseOrchestrator.class);

    private final AiClient aiClient;
    private final ResumePersistenceService persistence;
    private final CandidateEventPublisher eventPublisher;

    public ResumeParseOrchestrator(AiClient aiClient,
                                   ResumePersistenceService persistence,
                                   CandidateEventPublisher eventPublisher) {
        this.aiClient = aiClient;
        this.persistence = persistence;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Kicks off parsing on the {@code resumeParseExecutor} pool. {@code fileBytes}
     * are passed in (already read from storage) so the async task does not depend
     * on request-scoped state.
     */
    @Async("resumeParseExecutor")
    public void parseAsync(UUID candidateId, UUID resumeId, byte[] fileBytes,
                           String mimeType, String correlationId) {
        log.info("Starting async parse for resume {} (candidate {})", resumeId, candidateId);
        ParseResponse parsed;
        try {
            String base64 = Base64.getEncoder().encodeToString(fileBytes);
            parsed = aiClient.parse(new ParseRequest(null, base64, mimeType));
        } catch (RuntimeException ex) {
            log.warn("Parse failed for resume {}: {}", resumeId, ex.getMessage());
            persistence.markFailed(resumeId);
            return;
        }
        if (parsed == null) {
            log.warn("AI returned empty parse for resume {}", resumeId);
            persistence.markFailed(resumeId);
            return;
        }

        // Persist structured data (own transaction).
        try {
            persistence.persistStructuredData(candidateId, resumeId, parsed);
        } catch (RuntimeException ex) {
            log.error("Failed to persist parsed data for resume {}", resumeId, ex);
            persistence.markFailed(resumeId);
            return;
        }

        // Embed the resume text (best-effort; embedding ref is optional).
        UUID embeddingRef = null;
        String text = parsed.rawText();
        if (text != null && !text.isBlank()) {
            try {
                EmbedResponse embed = aiClient.embed(
                        new EmbedRequest("RESUME", resumeId.toString(), text));
                embeddingRef = parseUuid(embed == null ? null : embed.embeddingRef());
            } catch (RuntimeException ex) {
                log.warn("Embedding failed for resume {} (continuing as PARSED): {}",
                        resumeId, ex.getMessage());
            }
        }

        // Mark PARSED + store embedding ref (own transaction).
        persistence.markParsed(resumeId, embeddingRef);

        // Produce ResumeParsed to resume.parsed (key = candidateId).
        List<String> skills = parsed.skills() == null ? List.of() : parsed.skills();
        eventPublisher.publishResumeParsed(
                candidateId, resumeId, skills, parsed.totalExpYrs(), embeddingRef, correlationId);
        log.info("Resume {} parsed successfully ({} skills, embeddingRef={})",
                resumeId, skills.size(), embeddingRef);
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
