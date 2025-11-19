package org.example;

import org.example.db.Database;
import org.example.user.*;
import org.example.profile.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public class DemoMain {
    public static void main(String[] args) {

        System.out.println("=== Проверка подключения к PostgreSQL ===");
        try (Connection conn = Database.get()) {
            System.out.println("OK, подключились к: " + conn.getMetaData().getURL());
        } catch (SQLException e) {
            System.out.println("Ошибка подключения к БД:");
            e.printStackTrace();
            return;
        }

        UserRepository userRepo = new UserRepository();
        ProfileRepository profileRepo = new ProfileRepository();
        RegisterUser register = new RegisterUser(userRepo);

        register.register("test@example.com", "12345", "Мария");

        LoginUser login = new LoginUser(userRepo);
        login.login("test@example.com", "12345");

        Profile profile = new Profile(
                userRepo.findByEmail("test@example.com").orElseThrow().getId(),
                "Java Developer",
                Map.of("java", 1, "sql", 1)
                , 2
        );
        profileRepo.save(profile);

        System.out.println("Профиль из БД:");
        System.out.println(profileRepo.findByUserId(profile.getUserId()).orElseThrow());
    }
}
