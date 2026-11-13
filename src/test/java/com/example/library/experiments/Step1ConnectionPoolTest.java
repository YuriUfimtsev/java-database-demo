package com.example.library.experiments;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Эксперимент 1. Пул соединений HikariCP.
 *
 * Пока идут тесты, полезно смотреть на соединения со стороны PostgreSQL:
 *   select pid, application_name, state, query
 *   from pg_stat_activity where datname = 'library';
 */
@SpringBootTest
class Step1ConnectionPoolTest {

    @Autowired
    DataSourceProperties props;

    /** Сколько стоит открыть соединение без пула и с пулом. */
    @Test
    void driverManagerVsPool() throws Exception {
        int n = 20;

        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            try (Connection c = DriverManager.getConnection(props.getUrl(), props.getUsername(), props.getPassword())) {
                c.isValid(1);
            }
        }
        long withoutPool = ms(start);

        try (HikariDataSource ds = pool(5, 5_000)) {
            ds.getConnection().close(); // прогрев: пул создаёт соединения заранее
            start = System.nanoTime();
            for (int i = 0; i < n; i++) {
                try (Connection c = ds.getConnection()) { // close() вернёт соединение в пул
                    c.isValid(1);
                }
            }
            long withPool = ms(start);
            System.out.printf("%n>>> %d соединений: DriverManager %d мс, HikariCP %d мс%n%n", n, withoutPool, withPool);
        }
    }

    /**
     * В пуле 2 соединения, ждём не дольше 2 секунд.
     * Прогноз: что случится с третьим запросом?
     */
    @Test
    void thirdRequestFailsAfterTimeout() throws Exception {
        try (HikariDataSource ds = pool(2, 2_000)) {
            List<String> results = runThreeSlowQueries(ds);
            results.forEach(System.out::println);
            assertThat(results.get(2)).contains("SQLTransientConnectionException");
        }
    }

    /** Тот же опыт, но ждём 5 секунд: третий запрос дождётся соединения. */
    @Test
    void thirdRequestWaitsForFreeConnection() throws Exception {
        try (HikariDataSource ds = pool(2, 5_000)) {
            List<String> results = runThreeSlowQueries(ds);
            results.forEach(System.out::println);
            assertThat(results.get(2)).contains("получено");
        }
    }

    /** Соединение взяли и забыли закрыть. Hikari напишет WARN "Connection leak detection triggered". */
    @Test
    void connectionLeakIsDetected() throws Exception {
        HikariConfig config = baseConfig(2, 5_000);
        config.setLeakDetectionThreshold(2_000);
        try (HikariDataSource ds = new HikariDataSource(config)) {
            Connection leaked = ds.getConnection(); // нет try-with-resources
            Thread.sleep(3_000);
            System.out.printf("%n>>> Активных соединений: %d из %d%n%n",
                    ds.getHikariPoolMXBean().getActiveConnections(), config.getMaximumPoolSize());
            leaked.close();
        }
    }

    private List<String> runThreeSlowQueries(DataSource ds) throws Exception {
        long start = System.nanoTime();
        List<Future<String>> futures = new ArrayList<>();
        try (ExecutorService threads = Executors.newFixedThreadPool(3)) {
            for (int i = 1; i <= 3; i++) {
                int n = i;
                futures.add(threads.submit(() -> slowQuery(ds, n, start)));
                Thread.sleep(100); // чтобы порядок запросов был предсказуем
            }
            List<String> results = new ArrayList<>();
            for (Future<String> f : futures) {
                results.add(f.get());
            }
            return results;
        }
    }

    private String slowQuery(DataSource ds, int n, long start) {
        try (Connection c = ds.getConnection()) {
            long acquired = ms(start);
            try (Statement st = c.createStatement()) {
                st.execute("select pg_sleep(3)");
            }
            return "Запрос %d: соединение получено на %d мс, запрос завершён на %d мс".formatted(n, acquired, ms(start));
        } catch (SQLException e) {
            return "Запрос %d: %s на %d мс".formatted(n, e.getClass().getSimpleName(), ms(start));
        }
    }

    private HikariDataSource pool(int size, long connectionTimeoutMs) {
        return new HikariDataSource(baseConfig(size, connectionTimeoutMs));
    }

    private HikariConfig baseConfig(int size, long connectionTimeoutMs) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getUrl());
        config.setUsername(props.getUsername());
        config.setPassword(props.getPassword());
        config.setPoolName("tiny-pool");
        config.setMaximumPoolSize(size);
        config.setConnectionTimeout(connectionTimeoutMs);
        config.addDataSourceProperty("ApplicationName", "tiny-pool");
        return config;
    }

    private static long ms(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
