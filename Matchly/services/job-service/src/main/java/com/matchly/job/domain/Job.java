package com.matchly.job.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A job posting. Maps DATA_MODELS §3 {@code jobs}. */
@Entity
@Table(name = "jobs")
public class Job extends Auditable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The recruiter (Auth user) who owns the posting. Logical reference, not a FK. */
    @Column(name = "recruiter_id", nullable = false, updatable = false)
    private UUID recruiterId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "location")
    private String location;

    /** FULL_TIME | CONTRACT | INTERN. */
    @Column(name = "employment_type")
    private String employmentType;

    @Column(name = "min_exp_yrs")
    private Double minExpYrs;

    @Column(name = "max_exp_yrs")
    private Double maxExpYrs;

    /** HS | DIPLOMA | BACHELOR | MASTER | PHD. */
    @Column(name = "education_level")
    private String educationLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status = JobStatus.DRAFT;

    /** AI service vector id for the JD; set on publish. */
    @Column(name = "embedding_ref")
    private UUID embeddingRef;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected Job() {
        // for JPA
    }

    public Job(UUID id, UUID recruiterId, String title, String description) {
        this.id = id;
        this.recruiterId = recruiterId;
        this.title = title;
        this.description = description;
        this.status = JobStatus.DRAFT;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(UUID recruiterId) {
        this.recruiterId = recruiterId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public Double getMinExpYrs() {
        return minExpYrs;
    }

    public void setMinExpYrs(Double minExpYrs) {
        this.minExpYrs = minExpYrs;
    }

    public Double getMaxExpYrs() {
        return maxExpYrs;
    }

    public void setMaxExpYrs(Double maxExpYrs) {
        this.maxExpYrs = maxExpYrs;
    }

    public String getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(String educationLevel) {
        this.educationLevel = educationLevel;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public UUID getEmbeddingRef() {
        return embeddingRef;
    }

    public void setEmbeddingRef(UUID embeddingRef) {
        this.embeddingRef = embeddingRef;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}
