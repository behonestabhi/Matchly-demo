package com.matchly.interview.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A single interview question. Maps DATA_MODELS §6 {@code questions}. */
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_set_id", nullable = false)
    private QuestionSet questionSet;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private QuestionCategory category;

    @Column(name = "question", nullable = false)
    private String question;

    @Column(name = "suggested_answer")
    private String suggestedAnswer;

    @Column(name = "difficulty")
    private String difficulty;

    @Column(name = "ordinal")
    private Integer ordinal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Question() {
        // for JPA
    }

    public Question(UUID id, QuestionCategory category, String question,
                    String suggestedAnswer, String difficulty, Integer ordinal) {
        this.id = id;
        this.category = category;
        this.question = question;
        this.suggestedAnswer = suggestedAnswer;
        this.difficulty = difficulty;
        this.ordinal = ordinal;
    }

    public UUID getId() {
        return id;
    }

    public QuestionSet getQuestionSet() {
        return questionSet;
    }

    public void setQuestionSet(QuestionSet questionSet) {
        this.questionSet = questionSet;
    }

    public QuestionCategory getCategory() {
        return category;
    }

    public String getQuestion() {
        return question;
    }

    public String getSuggestedAnswer() {
        return suggestedAnswer;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public Integer getOrdinal() {
        return ordinal;
    }
}
