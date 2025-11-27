package com.aicareer.hh.infrastructure.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Провайдер подключения к PostgreSQL.
 * Использует параметры окружения из Docker:
 *  DB_URL, DB_USER, DB_PASSWORD
 */
public class DbConnectionProvider {
    private final String url;
    private final String user;
    private final String password;

    public DbConnectionProvider() {
        this.url = System.getenv("DB_URL");
        this.user = System.getenv("DB_USER");
        this.password = System.getenv("DB_PASSWORD");
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
