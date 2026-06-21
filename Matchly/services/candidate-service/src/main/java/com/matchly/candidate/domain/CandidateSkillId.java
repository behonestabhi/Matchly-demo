package com.matchly.candidate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite key for {@link CandidateSkill} ({@code candidate_id}, {@code skill_id}). */
@Embeddable
public class CandidateSkillId implements Serializable {

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    protected CandidateSkillId() {
        // for JPA
    }

    public CandidateSkillId(UUID candidateId, UUID skillId) {
        this.candidateId = candidateId;
        this.skillId = skillId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }

    public UUID getSkillId() {
        return skillId;
    }

    public void setSkillId(UUID skillId) {
        this.skillId = skillId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CandidateSkillId that)) {
            return false;
        }
        return Objects.equals(candidateId, that.candidateId)
                && Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(candidateId, skillId);
    }
}
