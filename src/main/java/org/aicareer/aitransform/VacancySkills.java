package org.aicareer.aitransform;

import java.util.Map;

public class VacancySkills {
    private Map<String, Integer> skills;

    public VacancySkills() {
    }

    public VacancySkills(Map<String, Integer> skills) {
        this.skills = skills;
    }

    public Map<String, Integer> getSkills() {
        return skills;
    }

    public void setSkills(Map<String, Integer> skills) {
        this.skills = skills;
    }

    @Override
    public String toString() {
        return "VacancySkills{" +
                "skills=" + skills +
                '}';
    }
}
