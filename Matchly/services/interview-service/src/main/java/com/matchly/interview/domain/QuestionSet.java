package com.matchly.interview.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A generated interview question set. Maps DATA_MODELS §6 {@code question_sets}. */
@Entity
@Table(name = "question_sets")
public class QuestionSet {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QuestionSetStatus status = QuestionSetStatus.GENERATING;

    @Column(name = "llm_model")
    private String llmModel;

    @Column(name = "prompt_version")
    private String promptVersion;

    @Column(name = "error_detail")
    private String errorDetail;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "questionSet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordinal ASC")
    private List<Question> questions = new ArrayList<>();

    protected QuestionSet() {
        // for JPA
    }

    public QuestionSet(UUID id, UUID candidateId, UUID jobId) {
        this.id = id;
        this.candidateId = candidateId;
        this.jobId = jobId;
        this.status = QuestionSetStatus.GENERATING;
    }

    public void addQuestion(Question question) {
        question.setQuestionSet(this);
        this.questions.add(question);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public QuestionSetStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionSetStatus status) {
        this.status = status;
    }

    public String getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(String llmModel) {
        this.llmModel = llmModel;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Question> getQuestions() {
        return questions;
    }
}
