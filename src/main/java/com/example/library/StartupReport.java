package com.example.library;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Печатает состояние базы при старте приложения.
 * Удобно, чтобы убедиться, что подключение и миграции работают.
 */
@Component
public class StartupReport implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public StartupReport(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        Long authors = jdbc.queryForObject("select count(*) from authors", Long.class);
        Long books = jdbc.queryForObject("select count(*) from books", Long.class);
        System.out.printf("%nБаза library: авторов %d, книг %d%n%n", authors, books);
    }
}
