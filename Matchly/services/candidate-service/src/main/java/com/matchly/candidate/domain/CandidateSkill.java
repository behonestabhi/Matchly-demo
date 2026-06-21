package com.matchly.candidate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** A skill attributed to a candidate. Maps DATA_MODELS §2 {@code candidate_skills}. */
@Entity
@Table(name = "candidate_skills")
public class CandidateSkill extends Auditable {

    @EmbeddedId
    private CandidateSkillId id;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    /** RESUME | SELF_DECLARED. */
    @Column(name = "source")
    private String source;

    protected CandidateSkill() {
        // for JPA
    }

    public CandidateSkill(UUID candidateId, UUID skillId, String skillName, String source) {
        this.id = new CandidateSkillId(candidateId, skillId);
        this.skillName = skillName;
        this.source = source;
    }

    public CandidateSkillId getId() {
        return id;
    }

    public void setId(CandidateSkillId id) {
        this.id = id;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
