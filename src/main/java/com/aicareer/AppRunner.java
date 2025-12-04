package com.aicareer;

import com.aicareer.aitransform.AppDatabaseInitializer;
import com.aicareer.aitransform.SkillsExtraction;
import com.aicareer.aitransform.UserInfoExporter;
import com.aicareer.comparison.Comparison;
import com.aicareer.comparison.Comparison.ComparisonResult;
import com.aicareer.hh.infrastructure.db.DbConnectionProvider;
import com.aicareer.recommendation.DeepseekRoadmapClient;
import com.aicareer.recommendation.RoadmapPromptBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

public class AppRunner {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .enable(SerializationFeature.INDENT_OUTPUT);

  private static final Path USER_MATRIX_PATH = Path.of("src/main/resources/matrices/user_skill_matrix.json");
  private static final Path ROLE_MATRIX_PATH = Path.of("src/main/resources/matrices/desired_role_matrix.json");
  private static final Path STATUSES_PATH = Path.of("src/main/resources/matrices/skill_comparison.json");
  private static final Path SUMMARY_PATH = Path.of("src/main/resources/matrices/summary.json");

  // дефолтный пользователь для quickstart
  private static final String DEFAULT_TEST_EMAIL = "test@example.com";

  public static void main(String[] args) {
    DbConnectionProvider provider = new DbConnectionProvider();

    System.out.println("=======================================");
    System.out.println("      AI-Career Navigator CLI");
    System.out.println("=======================================\n");

    System.out.println("[DB] applying schema and seeds...");
    new AppDatabaseInitializer(provider).applySchemaAndData();

    // старый режим: если есть аргументы — работаем как раньше, без диалогов
    if (args.length > 0) {
      runNonInteractive(provider, args);
      return;
    }

    // новый режим: диалог с пользователем в терминале
    try (Scanner in = new Scanner(System.in)) {
      runInteractive(provider, in);
    }
  }

  // ===== НЕИНТЕРАКТИВНЫЙ РЕЖИМ (как было) =====

  private static void runNonInteractive(DbConnectionProvider provider, String[] args) {
    String email = args.length > 0 ? args[0] : DEFAULT_TEST_EMAIL;
    String roleOverride = args.length > 1 ? args[1] : null;

    UserInfoExporter exporter = new UserInfoExporter(provider);

    UserInfoExporter.ProfileSnapshot profile = exporter.findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("User not found: " + email));

    String targetRole = roleOverride != null && !roleOverride.isBlank()
        ? roleOverride
        : profile.targetRole();

    if (targetRole == null || targetRole.isBlank()) {
      throw new IllegalStateException("Target role is empty for user: " + email);
    }

    System.out.println("[USER] loaded profile for: " + profile.name() + " (" + targetRole + ")");

    runPipeline(exporter, profile, targetRole);
  }

  // ===== ИНТЕРАКТИВНЫЙ РЕЖИМ =====

  private static void runInteractive(DbConnectionProvider provider, Scanner in) {
    UserInfoExporter exporter = new UserInfoExporter(provider);

    System.out.println("Как будем получать данные о пользователе?");
    System.out.println("1) Quickstart (дефолтный тестовый пользователь, никаких вопросов)");
    System.out.println("2) Выбрать тестового пользователя (ввести email)");
    System.out.println("3) Введу свои данные (ручной ввод, TODO)");
    System.out.print("> ");

    int mode = readInt(in, 1, 3);

    String email;
    switch (mode) {
      case 1 -> {
        System.out.println("[MODE] Quickstart");
        email = DEFAULT_TEST_EMAIL;
      }
      case 2 -> {
        System.out.println("[MODE] Тестовый пользователь по email");
        System.out.print("Введите email тестового пользователя (как в БД): ");
        email = in.nextLine().trim();
        while (email.isBlank()) {
          System.out.print("Email не может быть пустым, попробуйте ещё раз: ");
          email = in.nextLine().trim();
        }
      }
      case 3 -> {
        System.out.println("[MODE] Ручной ввод пока не реализован.");
        System.out.println("Можно будет добавить создание пользователя в БД или временный профиль.");
        return; // аккуратно выходим, не запускаем пайплайн
      }
      default -> throw new IllegalStateException("Unexpected mode: " + mode);
    }
    final String userEmail = email;

    UserInfoExporter.ProfileSnapshot profile = exporter.findByEmail(userEmail)
        .orElseThrow(() -> new IllegalStateException("User not found: " + userEmail));


    System.out.println("\n[USER] Загружен профиль: " + profile.name());
    System.out.println("      Email: " + email);
    System.out.println("      Целевая роль из анкеты: " + profile.targetRole());

    String targetRole = chooseTargetRole(in, profile.targetRole());

    System.out.println("\n[PIPELINE] Старт анализа для роли: " + targetRole);
    runPipeline(exporter, profile, targetRole);
  }

  // выбор желаемой роли
  private static String chooseTargetRole(Scanner in, String profileRole) {
    System.out.println("\nВыберите целевую роль:");
    System.out.println("1) Использовать роль из анкеты: " + profileRole);
    System.out.println("2) Выбрать из списка по вакансиям (vacancies_top25_*.json)");
    System.out.println("3) Ввести название роли вручную");
    System.out.print("> ");

    int choice = readInt(in, 1, 3);

    switch (choice) {
      case 1 -> {
        return profileRole;
      }
      case 2 -> {
        List<String> roles = listAvailableRoles();
        if (roles.isEmpty()) {
          System.out.println("[ROLE] Не удалось найти файлы vacancies_top25_*.json, используем роль из анкеты.");
          return profileRole;
        }
        System.out.println("\nДоступные роли по данным о вакансиях:");
        for (int i = 0; i < roles.size(); i++) {
          System.out.println((i + 1) + ") " + roles.get(i));
        }
        System.out.print("Введите номер роли (0 — вернуться к роли из анкеты): ");

        int idx = readInt(in, 0, roles.size());
        if (idx == 0) {
          return profileRole;
        }
        return roles.get(idx - 1);
      }
      case 3 -> {
        System.out.print("Введите название роли (например, Java Developer): ");
        String role = in.nextLine().trim();
        while (role.isBlank()) {
          System.out.print("Роль не может быть пустой, попробуйте ещё раз: ");
          role = in.nextLine().trim();
        }
        return role;
      }
      default -> throw new IllegalStateException("Unexpected value: " + choice);
    }
  }

  // поиск всех файлов vacancies_top25_*.json и превращение их в человекочитаемые названия ролей
  private static List<String> listAvailableRoles() {
    Path exportDir = Path.of("src/main/resources/export");
    if (!Files.isDirectory(exportDir)) {
      return List.of();
    }

    try (Stream<Path> stream = Files.list(exportDir)) {
      return stream
          .filter(Files::isRegularFile)
          .map(path -> path.getFileName().toString())
          .filter(name -> name.startsWith("vacancies_top25_") && name.endsWith(".json"))
          .sorted()
          .map(name -> name.substring(
              "vacancies_top25_".length(),
              name.length() - ".json".length()
          ))
          .map(safe -> safe.replace('_', ' ')) // "middle_java_developer" -> "middle java developer"
          .toList();
    } catch (IOException e) {
      System.err.println("[ROLE] Не удалось прочитать список файлов в " + exportDir + ": " + e.getMessage());
      return List.of();
    }
  }

  // ===== ОБЩИЙ ПАЙПЛАЙН (как был, но вынесен в отдельный метод) =====

  private static void runPipeline(UserInfoExporter exporter,
      UserInfoExporter.ProfileSnapshot profile,
      String targetRole) {

    if (targetRole == null || targetRole.isBlank()) {
      throw new IllegalStateException("Target role is empty for user: " + profile.name());
    }

    exporter.writeUserSkillMatrix(profile.skills(), USER_MATRIX_PATH);

    String vacanciesResource = resolveVacanciesResource(targetRole);
    System.out.println("[ROLE] using vacancies resource: " + vacanciesResource);

    Map<String, Integer> roleMatrix = SkillsExtraction.fromResource(vacanciesResource);
    writeJson(ROLE_MATRIX_PATH, roleMatrix);

    ComparisonResult comparison = Comparison.calculate(roleMatrix, profile.skills());
    Comparison.writeOutputs(comparison, STATUSES_PATH, SUMMARY_PATH);

    System.out.println("[COMPARE] Strong sides: " +
        comparison.summary().getOrDefault("лучше ожидаемого", List.of()));
    System.out.println("[COMPARE] Weak sides:   " +
        comparison.summary().getOrDefault("требует улучшения", List.of()));

    String prompt = RoadmapPromptBuilder.build(
        vacanciesResource,
        "matrices/user_skill_matrix.json",
        "matrices/desired_role_matrix.json",
        "graphs/skills-graph.json"
    );

    try {
      String roadmap = DeepseekRoadmapClient.generateRoadmap(prompt);
      System.out.println("\n[AI RESPONSE]\n" + roadmap);
    } catch (Exception e) {
      System.err.println("[AI] Failed to get roadmap from model: " + e.getMessage());
    }
  }

  // ===== СТАРЫЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====

  private static String resolveVacanciesResource(String roleName) {
    String safe = roleName.toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "_")
        .replaceAll("^_+|_+$", "");
    String resource = "export/vacancies_top25_" + safe + ".json";

    if (resourceExists(resource)) {
      return resource;
    }
    throw new IllegalArgumentException("No vacancies_top25 resource found for role: " + roleName);
  }

  private static boolean resourceExists(String resource) {
    return Thread.currentThread()
        .getContextClassLoader()
        .getResource(resource) != null;
  }

  private static void writeJson(Path path, Object payload) {
    try {
      if (path.getParent() != null) {
        Files.createDirectories(path.getParent());
      }
      MAPPER.writeValue(path.toFile(), payload);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to write JSON to " + path, e);
    }
  }

  // ===== УТИЛИТА ДЛЯ ЧТЕНИЯ ЦЕЛЫХ ЧИСЕЛ ИЗ КОНСОЛИ =====

  private static int readInt(Scanner in, int min, int max) {
    while (true) {
      String line = in.nextLine().trim();
      try {
        int value = Integer.parseInt(line);
        if (value < min || value > max) {
          System.out.print("Введите число от " + min + " до " + max + ": ");
          continue;
        }
        return value;
      } catch (NumberFormatException e) {
        System.out.print("Некорректный ввод, введите число от " + min + " до " + max + ": ");
      }
    }
  }
}
