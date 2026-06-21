package com.matchly.candidate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Structured parse result for a resume. Maps DATA_MODELS §2 {@code parsed_resumes}. */
@Entity
@Table(name = "parsed_resumes")
public class ParsedResume extends Auditable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "resume_id", nullable = false, updatable = false, unique = true)
    private UUID resumeId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "total_exp_yrs")
    private BigDecimal totalExpYrs;

    @Column(name = "raw_text")
    private String rawText;

    protected ParsedResume() {
        // for JPA
    }

    public ParsedResume(UUID id, UUID resumeId) {
        this.id = id;
        this.resumeId = resumeId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getResumeId() {
        return resumeId;
    }

    public void setResumeId(UUID resumeId) {
        this.resumeId = resumeId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public BigDecimal getTotalExpYrs() {
        return totalExpYrs;
    }

    public void setTotalExpYrs(BigDecimal totalExpYrs) {
        this.totalExpYrs = totalExpYrs;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }
}
