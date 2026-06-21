package com.matchly.job.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** A skill required by a job. Maps DATA_MODELS §3 {@code job_required_skills}. */
@Entity
@Table(name = "job_required_skills")
public class JobRequiredSkill extends Auditable {

    @EmbeddedId
    private JobRequiredSkillId id;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    /** Importance used by the matching score (0..1+). */
    @Column(name = "weight")
    private Double weight;

    /** Must-have ({@code true}) vs nice-to-have ({@code false}). */
    @Column(name = "required")
    private Boolean required;

    protected JobRequiredSkill() {
        // for JPA
    }

    public JobRequiredSkill(UUID jobId, UUID skillId, String skillName, Double weight, Boolean required) {
        this.id = new JobRequiredSkillId(jobId, skillId);
        this.skillName = skillName;
        this.weight = weight;
        this.required = required;
    }

    public JobRequiredSkillId getId() {
        return id;
    }

    public void setId(JobRequiredSkillId id) {
        this.id = id;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }
}
