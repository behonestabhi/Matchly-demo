package com.matchly.analytics.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.matchly.analytics.service.ProjectionService;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Null-safe extraction helpers for reading fields out of a payload {@link JsonNode}. */
public final class PayloadReader {

    private static final Logger log = LoggerFactory.getLogger(PayloadReader.class);

    private PayloadReader() {
    }

    public static UUID uuid(JsonNode node, String field) {
        String raw = text(node, field);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Field '{}' value '{}' is not a UUID", field, raw);
            return null;
        }
    }

    public static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    public static Double doubleVal(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asDouble();
    }

    /** Parse an ISO-8601 timestamp; falls back to the envelope-provided value. */
    public static Instant instant(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(iso.trim());
        } catch (DateTimeParseException e) {
            log.warn("Unparseable timestamp '{}'", iso);
            return null;
        }
    }

    /**
     * Read a {@code requiredSkills} array of either strings or
     * {@code {skillId?, skillName}} / {@code {id?, name}} objects.
     */
    public static List<ProjectionService.SkillRef> skills(JsonNode payload) {
        List<ProjectionService.SkillRef> out = new ArrayList<>();
        if (payload == null) {
            return out;
        }
        JsonNode arr = payload.get("requiredSkills");
        if (arr == null || !arr.isArray()) {
            return out;
        }
        for (JsonNode el : arr) {
            if (el.isTextual()) {
                out.add(new ProjectionService.SkillRef(null, el.asText()));
            } else if (el.isObject()) {
                String name = firstText(el, "skillName", "name");
                UUID id = firstUuid(el, "skillId", "id");
                if (name != null && !name.isBlank()) {
                    out.add(new ProjectionService.SkillRef(id, name));
                }
            }
        }
        return out;
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String f : fields) {
            String v = text(node, f);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static UUID firstUuid(JsonNode node, String... fields) {
        for (String f : fields) {
            UUID v = uuid(node, f);
            if (v != null) {
                return v;
            }
        }
        return null;
    }
}
