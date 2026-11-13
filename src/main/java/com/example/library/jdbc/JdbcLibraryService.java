package com.example.library.jdbc;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Ручное управление транзакциями в JDBC.
 * Сценарий: добавить автора и его книгу. Либо обе строки, либо ни одной.
 */
@Service
public class JdbcLibraryService {

    private final DataSource dataSource;

    public JdbcLibraryService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Без транзакции: каждый INSERT фиксируется сразу (autoCommit = true). */
    public long addAuthorWithBookAutoCommit(String authorName, String title, boolean failInBetween) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            long authorId = insertAuthor(connection, authorName);
            if (failInBetween) {
                throw new IllegalStateException("Сбой между двумя INSERT");
            }
            insertBook(connection, title, authorId);
            return authorId;
        }
    }

    /** С транзакцией: commit() в конце, rollback() при любой ошибке. */
    public long addAuthorWithBook(String authorName, String title, boolean failInBetween) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);                   // начало транзакции
            try {
                long authorId = insertAuthor(connection, authorName);
                if (failInBetween) {
                    throw new IllegalStateException("Сбой между двумя INSERT");
                }
                insertBook(connection, title, authorId);
                connection.commit();
                return authorId;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                // Соединение вернётся в пул и достанется другому коду.
                // HikariCP сам восстанавливает autoCommit, но полагаться на это не стоит.
                connection.setAutoCommit(true);
            }
        }
    }

    private static long insertAuthor(Connection connection, String name) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(
                "insert into authors (name) values (?)", Statement.RETURN_GENERATED_KEYS)) {
            st.setString(1, name);
            st.executeUpdate();
            try (ResultSet keys = st.getGeneratedKeys()) {
                keys.next();
                return keys.getLong("id");
            }
        }
    }

    private static void insertBook(Connection connection, String title, long authorId) throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(
                "insert into books (title, author_id) values (?, ?)")) {
            st.setString(1, title);
            st.setLong(2, authorId);
            st.executeUpdate();
        }
    }
}
