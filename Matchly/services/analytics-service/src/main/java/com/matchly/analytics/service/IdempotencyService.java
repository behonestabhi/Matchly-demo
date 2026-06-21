package com.matchly.analytics.service;

import com.matchly.analytics.domain.ProcessedEvent;
import com.matchly.analytics.repository.ProcessedEventRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simple consumer-side idempotency: record each handled {@code eventId} so
 * replays are no-ops.
 *
 * <p>SIMPLIFICATION (noted in DATA_MODELS §9): the canonical design dedups in
 * Redis. Here we use a small DB table; the unique PK on {@code event_id} makes
 * the claim atomic even under concurrent delivery.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final ProcessedEventRepository processedEventRepository;

    public IdempotencyService(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Atomically claim an event id for processing.
     *
     * @return {@code true} if this is the first time we have seen the id (caller
     *     should process it); {@code false} if it was already processed.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean claim(String eventIdRaw, String eventType) {
        if (eventIdRaw == null || eventIdRaw.isBlank()) {
            // No id to dedup on — process it but warn (best-effort).
            log.warn("Event of type {} has no eventId; processing without dedup", eventType);
            return true;
        }
        UUID eventId;
        try {
            eventId = UUID.fromString(eventIdRaw.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Event {} has non-UUID eventId '{}'; processing without dedup", eventType, eventIdRaw);
            return true;
        }
        if (processedEventRepository.existsByEventId(eventId)) {
            return false;
        }
        try {
            processedEventRepository.save(new ProcessedEvent(eventId, eventType));
            return true;
        } catch (DataIntegrityViolationException e) {
            // Lost a race to a concurrent consumer; treat as already processed.
            return false;
        }
    }
}
