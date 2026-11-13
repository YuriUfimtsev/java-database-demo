package com.example.library.jdbc;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Доступ к данным через чистый JDBC.
 * DataSource здесь это пул HikariCP, который настроил Spring Boot.
 */
@Repository
public class JdbcBookDao {

    private static final String SELECT_BOOKS = """
            select b.id, b.title, a.name as author_name
            from books b
                join authors a on a.id = b.author_id
            """;

    private final DataSource dataSource;

    public JdbcBookDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<BookRow> findAll() throws SQLException {
        try (Connection connection = dataSource.getConnection();   // взяли соединение из пула
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(SELECT_BOOKS + " order by b.id")) {
            return readRows(rs);
        }                                                           // close() вернул соединение в пул
    }

    /** Правильно: параметр передаётся отдельно от текста запроса. */
    public List<BookRow> findByTitle(String title) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BOOKS + " where b.title = ?")) {
            statement.setString(1, title);
            try (ResultSet rs = statement.executeQuery()) {
                return readRows(rs);
            }
        }
    }

    /** Неправильно: строка пользователя становится частью SQL. Попробуйте title = "x' or '1'='1". */
    public List<BookRow> findByTitleUnsafe(String title) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(SELECT_BOOKS + " where b.title = '" + title + "'")) {
            return readRows(rs);
        }
    }

    /**
     * Имя колонки параметром не передать: "order by ?" сортирует по константе.
     * Поэтому идентификаторы выбираем только из белого списка.
     */
    public List<BookRow> findAllSorted(String sortBy) throws SQLException {
        String column = switch (sortBy) {
            case "id" -> "b.id";
            case "title" -> "b.title";
            case "author" -> "a.name";
            default -> throw new IllegalArgumentException("Нельзя сортировать по " + sortBy);
        };
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(SELECT_BOOKS + " order by " + column)) {
            return readRows(rs);
        }
    }

    private static List<BookRow> readRows(ResultSet rs) throws SQLException {
        List<BookRow> result = new ArrayList<>();
        while (rs.next()) {                       // курсор стоит перед первой строкой
            result.add(new BookRow(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("author_name")));
        }
        return result;
    }
}
