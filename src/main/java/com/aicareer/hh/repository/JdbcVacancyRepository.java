package com.aicareer.hh.repository;

import com.aicareer.hh.infrastructure.db.DbConnectionProvider;
import com.aicareer.hh.model.Vacancy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Репозиторий для сохранения вакансий в PostgreSQL.
 */
public class JdbcVacancyRepository {

    private final DbConnectionProvider provider;

    public JdbcVacancyRepository(DbConnectionProvider provider) {
        this.provider = provider;
    }

    public void saveAll(List<Vacancy> vacancies) {
        String sql = """
            INSERT INTO vacancy (
                external_id, title, company, city,
                experience, employment, schedule,
                salary_from, salary_to, currency,
                description, url, source, published_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (external_id) DO UPDATE
            SET title = EXCLUDED.title,
                company = EXCLUDED.company,
                city = EXCLUDED.city,
                experience = EXCLUDED.experience,
                employment = EXCLUDED.employment,
                schedule = EXCLUDED.schedule,
                salary_from = EXCLUDED.salary_from,
                salary_to = EXCLUDED.salary_to,
                currency = EXCLUDED.currency,
                description = EXCLUDED.description,
                url = EXCLUDED.url,
                source = EXCLUDED.source,
                published_at = EXCLUDED.published_at;
        """;

        try (Connection conn = provider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (Vacancy v : vacancies) {
                ps.setString(1, v.getId());
                ps.setString(2, v.getTitle());
                ps.setString(3, v.getCompany());
                ps.setString(4, v.getCity());
                ps.setString(5, v.getExperience());
                ps.setString(6, v.getEmployment());
                ps.setString(7, v.getSchedule());
                ps.setObject(8, v.getSalaryFrom());
                ps.setObject(9, v.getSalaryTo());
                ps.setString(10, v.getCurrency());
                ps.setString(11, v.getDescription());
                ps.setString(12, v.getUrl());
                ps.setString(13, v.getSource());
                ps.setString(14, v.getPublishedAt() != null ? v.getPublishedAt().toString() : null);
                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при сохранении вакансий в БД", e);
        }
    }
}
