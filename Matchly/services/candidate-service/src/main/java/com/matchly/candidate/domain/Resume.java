package com.matchly.candidate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** An uploaded resume. Maps DATA_MODELS §2 {@code resumes}. */
@Entity
@Table(name = "resumes")
public class Resume extends Auditable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "candidate_id", nullable = false, updatable = false)
    private UUID candidateId;

    /** Storage key for the raw bytes. Here a relative path on local disk (stands in for S3). */
    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    /** PENDING | PARSED | FAILED. */
    @Column(name = "parse_status", nullable = false)
    private String parseStatus = "PENDING";

    /** Id of the vector row in the AI service (set after embedding). */
    @Column(name = "embedding_ref")
    private UUID embeddingRef;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = true;

    protected Resume() {
        // for JPA
    }

    public Resume(UUID id, UUID candidateId, String s3Key, String fileName, String mimeType) {
        this.id = id;
        this.candidateId = candidateId;
        this.s3Key = s3Key;
        this.fileName = fileName;
        this.mimeType = mimeType;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }

    public UUID getEmbeddingRef() {
        return embeddingRef;
    }

    public void setEmbeddingRef(UUID embeddingRef) {
        this.embeddingRef = embeddingRef;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}
