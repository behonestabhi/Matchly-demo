package com.matchly.analytics.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Composite key for {@link SkillDemand}: (day, skill_id). */
public class SkillDemandId implements Serializable {

    private LocalDate day;
    private UUID skillId;

    public SkillDemandId() {
    }

    public SkillDemandId(LocalDate day, UUID skillId) {
        this.day = day;
        this.skillId = skillId;
    }

    public LocalDate getDay() {
        return day;
    }

    public UUID getSkillId() {
        return skillId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SkillDemandId that)) {
            return false;
        }
        return Objects.equals(day, that.day) && Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, skillId);
    }
}
