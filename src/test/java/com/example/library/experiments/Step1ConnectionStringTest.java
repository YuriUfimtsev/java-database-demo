package com.example.library.experiments;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Эксперимент 1а. Строка подключения и параметры драйвера PostgreSQL.
 *
 * jdbc:postgresql://localhost:5432/library?ApplicationName=...&connectTimeout=5
 * Параметр options передаёт настройки самому серверу, как SET в начале сеанса.
 */
@SpringBootTest
class Step1ConnectionStringTest {

    @Autowired
    DataSourceProperties props;

    @Test
    void urlParameters() throws Exception {
        String url = props.getUrl()
                + "?ApplicationName=url-demo"
                + "&currentSchema=public"
                + "&connectTimeout=5"
                + "&options=-c%20statement_timeout=2s";

        try (Connection c = DriverManager.getConnection(url, props.getUsername(), props.getPassword());
             Statement st = c.createStatement()) {

            System.out.println(">>> application_name  = " + show(st, "show application_name"));
            System.out.println(">>> current_schema    = " + show(st, "select current_schema()"));
            System.out.println(">>> statement_timeout = " + show(st, "show statement_timeout"));

            assertThat(show(st, "show application_name")).isEqualTo("url-demo");
            assertThat(show(st, "show statement_timeout")).isEqualTo("2s");
        }
    }

    /** statement_timeout из строки подключения: сервер сам отменит долгий запрос. */
    @Test
    void statementTimeoutFromUrl() throws Exception {
        String url = props.getUrl() + "?options=-c%20statement_timeout=2s";
        try (Connection c = DriverManager.getConnection(url, props.getUsername(), props.getPassword());
             Statement st = c.createStatement()) {
            assertThatThrownBy(() -> st.execute("select pg_sleep(3)"))
                    .isInstanceOf(SQLException.class)
                    .satisfies(e -> System.out.println(">>> " + ((SQLException) e).getSQLState() + " " + e.getMessage()));
        }
    }

    private static String show(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }
}
