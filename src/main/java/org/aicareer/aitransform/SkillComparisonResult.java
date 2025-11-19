package org.example;

import java.util.Map;

public class SkillComparisonResult {

    private final Map<String, Integer> matched;
    private final Map<String, Integer> missing;
    private final Map<String, Integer> extra;

    public SkillComparisonResult(
            Map<String, Integer> matched,
            Map<String, Integer> missing,
            Map<String, Integer> extra
    ) {
        this.matched = Map.copyOf(matched);
        this.missing = Map.copyOf(missing);
        this.extra = Map.copyOf(extra);
    }

    public Map<String, Integer> getMatched() {
        return matched;
    }

    public Map<String, Integer> getMissing() {
        return missing;
    }

    public Map<String, Integer> getExtra() {
        return extra;
    }

    @Override
    public String toString() {
        return "SkillComparisonResult{" +
                "matched=" + matched +
                ", missing=" + missing +
                ", extra=" + extra +
                '}';
    }
}
