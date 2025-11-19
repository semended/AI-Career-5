package org.example.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.db.Database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class ProfileRepository {
    private final ObjectMapper mapper = new ObjectMapper();

    public void save(Profile profile) {
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);

            String upsertProfile = """
                INSERT INTO app_profiles (user_id, target_role, experience_years, updated_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id)
                DO UPDATE SET target_role = EXCLUDED.target_role,
                              experience_years = EXCLUDED.experience_years,
                              updated_at = EXCLUDED.updated_at
            """;

            try (PreparedStatement ps = c.prepareStatement(upsertProfile)) {
                ps.setString(1, profile.getUserId());
                ps.setString(2, profile.getTargetRole());
                ps.setInt(3, profile.getExperienceYears());
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            }

            String deleteSkills = "DELETE FROM app_profile_skills WHERE profile_id = ?";
            try (PreparedStatement ps = c.prepareStatement(deleteSkills)) {
                ps.setString(1, profile.getUserId());
                ps.executeUpdate();
            }


            String insertSkill = """
                INSERT INTO app_profile_skills (profile_id, skill_key, level)
                VALUES (?, ?, ?)
            """;

            try (PreparedStatement ps = c.prepareStatement(insertSkill)) {
        for (Map.Entry<String, Integer> entry : profile.getSkills().entrySet()) {
        ps.setString(1, profile.getUserId());
        ps.setString(2, entry.getKey());
        ps.setInt(3, entry.getValue());
        ps.addBatch();
                }
                        ps.executeBatch();
            }

                    c.commit();
        } catch (SQLException e) {
        throw new RuntimeException("Ошибка при сохранении профиля", e);
        }
                }


public Optional<Profile> findByUserId(String userId) {
    try (Connection c = Database.get()) {

        String profileSql = "SELECT * FROM app_profiles WHERE user_id = ?";
        String skillsSql = "SELECT skill_key, level FROM app_profile_skills WHERE profile_id = ?";

        PreparedStatement psProf = c.prepareStatement(profileSql);
        psProf.setString(1, userId);
        ResultSet rsProf = psProf.executeQuery();

        if (!rsProf.next()) return Optional.empty();

        Map<String, Integer> skills = new HashMap<>();
        PreparedStatement psSkills = c.prepareStatement(skillsSql);
        psSkills.setString(1, userId);
        ResultSet rsSkills = psSkills.executeQuery();

        while (rsSkills.next()) {
            skills.put(rsSkills.getString("skill_key"), rsSkills.getInt("level"));
        }

        return Optional.of(new Profile(
                rsProf.getString("user_id"),
                rsProf.getString("target_role"),
                skills,
                rsProf.getInt("experience_years")
        ));

    } catch (SQLException e) {
        throw new RuntimeException("Ошибка при загрузке профиля", e);
    }
}


public List<Profile> findAll() {
    List<Profile> result = new ArrayList<>();
    try (Connection c = Database.get()) {

        String profileSql = "SELECT * FROM app_profiles";
        String skillsSql = "SELECT skill_key, level FROM app_profile_skills WHERE profile_id = ?";

        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery(profileSql);

        while (rs.next()) {
            String userId = rs.getString("user_id");

            Map<String, Integer> skills = new HashMap<>();
            PreparedStatement psSkills = c.prepareStatement(skillsSql);
            psSkills.setString(1, userId);
            ResultSet rsSkills = psSkills.executeQuery();

            while (rsSkills.next()) {
                skills.put(rsSkills.getString("skill_key"), rsSkills.getInt("level"));
            }

            result.add(new Profile(
                    userId,
                    rs.getString("target_role"),
                    skills,
                    rs.getInt("experience_years")
            ));
        }

    } catch (SQLException e) {
        throw new RuntimeException("Ошибка при загрузке всех профилей", e);
    }
    return result;
}
}
