package org.example;

import java.sql.*;
import java.util.HashMap;
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
    public static Map<String, Integer> loadUserSkillsFromDb(Connection connection, String userId) throws SQLException {

        Map<String, Integer> skillMap = new HashMap<>();

        String sql = """
            SELECT ps.skill_key, ps.level
            FROM profiles p
            JOIN profile_skills ps ON ps.profile_id = p.id
            WHERE p.user_id = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String skill = rs.getString("skill_key");
                int level = rs.getInt("level");
                skillMap.put(skill.toLowerCase(), level);   // привели ключи к нижнему регистру
            }
        }

        return skillMap;
    }


    /** 
     * userSkills — реальные навыки пользователя из БД
     * roleSkills — усечённая матрица роли из модели (0/1)
     */
    public static SkillComparisonResult compare(
            Map<String, Integer> userSkills,
            Map<String, Integer> roleSkills
    ) {

        Map<String, Integer> matched = new HashMap<>();
        Map<String, Integer> missing = new HashMap<>();
        Map<String, Integer> extra = new HashMap<>();

        // что требуется ролью
        for (var entry : roleSkills.entrySet()) {
            String skill = entry.getKey().toLowerCase();
            int required = entry.getValue();
            int actual = userSkills.getOrDefault(skill, 0);

            if (required == 1 && actual == 1) {
                matched.put(skill, 1);
            } else if (required == 1 && actual == 0) {
                missing.put(skill, 1);
            }
        }

        // что пользователь знает сверх требований роли
        for (var entry : userSkills.entrySet()) {
            String skill = entry.getKey();
            if (!roleSkills.containsKey(skill)) {
                extra.put(skill, entry.getValue());
            }
        }

        return new SkillComparisonResult(matched, missing, extra);
    }
}
