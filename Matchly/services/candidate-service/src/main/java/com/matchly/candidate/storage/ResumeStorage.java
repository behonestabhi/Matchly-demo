package com.matchly.candidate.storage;

import com.matchly.candidate.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores raw resume bytes on local disk. This <em>stands in for S3</em>: the
 * returned key is a relative path recorded as the resume's {@code s3Key}, and in
 * a real deployment this component would be swapped for an S3 client. The base
 * directory is configurable via {@code RESUME_STORAGE_DIR}.
 */
@Component
public class ResumeStorage {

    private static final Logger log = LoggerFactory.getLogger(ResumeStorage.class);

    private final Path baseDir;

    public ResumeStorage(@Value("${resume.storage-dir:./data/resumes}") String storageDir) {
        this.baseDir = Paths.get(storageDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(baseDir);
            log.info("Resume storage directory: {}", baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create resume storage dir: " + baseDir, e);
        }
    }

    /**
     * Persist the uploaded file and return its storage key (relative path,
     * recorded as {@code s3Key}). Layout: {@code <candidateId>/<resumeId>-<filename>}.
     */
    public String store(UUID candidateId, UUID resumeId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Resume file is required and must not be empty");
        }
        String safeName = sanitize(file.getOriginalFilename());
        String key = candidateId + "/" + resumeId + "-" + safeName;
        Path target = baseDir.resolve(key).normalize();
        if (!target.startsWith(baseDir)) {
            throw new BadRequestException("Invalid resume file name");
        }
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store resume bytes for " + key, e);
        }
        return key;
    }

    /** Read previously stored bytes by key. */
    public byte[] read(String key) {
        Path target = baseDir.resolve(key).normalize();
        if (!target.startsWith(baseDir)) {
            throw new BadRequestException("Invalid storage key");
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read resume bytes for " + key, e);
        }
    }

    private static String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "resume";
        }
        // Strip any path components and keep a filesystem-safe basename.
        String base = Paths.get(name).getFileName().toString();
        return base.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
