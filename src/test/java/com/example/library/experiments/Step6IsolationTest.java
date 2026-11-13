package com.example.library.experiments;

import com.example.library.service.IsolationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Эксперимент 6б. Уровни изоляции в PostgreSQL.
 *
 * Две транзакции на двух соединениях из пула. Шаги чередуются в одном потоке,
 * поэтому порядок всегда одинаковый. T2 работает в autoCommit.
 */
class Step6IsolationTest extends ExperimentBase {

    @Autowired
    DataSource dataSource;

    @Autowired
    IsolationService isolationService;

    /** Прогноз: увидит ли T1 изменения T2, если перечитает данные? */
    @Test
    void readCommitted() throws Exception {
        Reads r = readTwice(Connection.TRANSACTION_READ_COMMITTED);
        step("READ COMMITTED: " + r);
        assertThat(r.secondTitle).isEqualTo("Война и мир. Том 1"); // неповторяющееся чтение
        assertThat(r.secondCount).isEqualTo(4);                     // фантом
    }

    @Test
    void repeatableRead() throws Exception {
        Reads r = readTwice(Connection.TRANSACTION_REPEATABLE_READ);
        step("REPEATABLE READ: " + r);
        assertThat(r.secondTitle).isEqualTo("Война и мир");          // снимок данных
        assertThat(r.secondCount).isEqualTo(3);                      // фантомов тоже нет
    }

    /** Потерянное обновление: T1 молча затирает изменение T2. */
    @Test
    void lostUpdateInReadCommitted() throws Exception {
        updateAfterConcurrentUpdate(Connection.TRANSACTION_READ_COMMITTED);
        step("Итоговое название: " + titleOfBook(1));
        assertThat(titleOfBook(1)).isEqualTo("Изменил T1");
    }

    /** На REPEATABLE READ PostgreSQL обнаруживает конфликт. Транзакцию нужно повторить. */
    @Test
    void conflictInRepeatableRead() {
        assertThatThrownBy(() -> updateAfterConcurrentUpdate(Connection.TRANSACTION_REPEATABLE_READ))
                .isInstanceOf(SQLException.class)
                .satisfies(e -> {
                    step(((SQLException) e).getSQLState() + " " + e.getMessage());
                    assertThat(((SQLException) e).getSQLState()).isEqualTo("40001");
                });
        assertThat(titleOfBook(1)).isEqualTo("Изменил T2");
    }

    /** Уровень изоляции через @Transactional. */
    @Test
    void isolationInSpring() {
        step("По умолчанию: " + isolationService.defaultLevel());
        step("REPEATABLE_READ: " + isolationService.repeatableRead());
        assertThat(isolationService.defaultLevel()).isEqualTo("read committed");
        assertThat(isolationService.repeatableRead()).isEqualTo("repeatable read");
    }

    // ---------------------------------------------------------------

    record Reads(String firstTitle, String secondTitle, long firstCount, long secondCount) {
    }

    private Reads readTwice(int level) throws Exception {
        try (Connection t1 = dataSource.getConnection(); Connection t2 = dataSource.getConnection()) {
            t1.setAutoCommit(false);
            t1.setTransactionIsolation(level);
            try {
                String title1 = title(t1);
                long count1 = booksOfTolstoy(t1);

                t2.createStatement().executeUpdate("update books set title = 'Война и мир. Том 1' where id = 1");
                t2.createStatement().executeUpdate("insert into books (title, author_id) values ('Хаджи-Мурат', 1)");

                String title2 = title(t1);
                long count2 = booksOfTolstoy(t1);
                t1.commit();
                return new Reads(title1, title2, count1, count2);
            } finally {
                t1.setAutoCommit(true);
            }
        }
    }

    private void updateAfterConcurrentUpdate(int level) throws Exception {
        try (Connection t1 = dataSource.getConnection(); Connection t2 = dataSource.getConnection()) {
            t1.setAutoCommit(false);
            t1.setTransactionIsolation(level);
            try {
                title(t1);                                                                   // T1 прочитала
                t2.createStatement().executeUpdate("update books set title = 'Изменил T2' where id = 1");
                t1.createStatement().executeUpdate("update books set title = 'Изменил T1' where id = 1");
                t1.commit();
            } catch (SQLException e) {
                t1.rollback();
                throw e;
            } finally {
                t1.setAutoCommit(true);
            }
        }
    }

    private static String title(Connection c) throws SQLException {
        try (ResultSet rs = c.createStatement().executeQuery("select title from books where id = 1")) {
            rs.next();
            return rs.getString(1);
        }
    }

    private static long booksOfTolstoy(Connection c) throws SQLException {
        try (ResultSet rs = c.createStatement().executeQuery("select count(*) from books where author_id = 1")) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
