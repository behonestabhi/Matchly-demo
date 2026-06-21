package com.matchly.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Per (day, skill) job count from JobPosted requiredSkills
 * (DATA_MODELS §7 {@code skill_demand}). */
@Entity
@Table(name = "skill_demand")
@IdClass(SkillDemandId.class)
public class SkillDemand {

    @Id
    @Column(name = "day", nullable = false)
    private LocalDate day;

    @Id
    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    @Column(name = "job_count", nullable = false)
    private int jobCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SkillDemand() {
    }

    public SkillDemand(LocalDate day, UUID skillId, String skillName) {
        this.day = day;
        this.skillId = skillId;
        this.skillName = skillName;
    }

    public void increment() {
        this.jobCount++;
    }

    public LocalDate getDay() {
        return day;
    }

    public UUID getSkillId() {
        return skillId;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public int getJobCount() {
        return jobCount;
    }
}
