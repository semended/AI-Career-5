package org.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static final String URL =
            "jdbc:postgresql://localhost:5432/aicareer?sslmode=disable&applicationName=AI-Career-Client";

    private static final String USER = "aicareer";
    private static final String PASSWORD = "aicareer";

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}


