package com.example.library.experiments;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Общая основа экспериментов.
 * Перед каждым тестом база возвращается в исходное состояние (reset.sql).
 * Тесты работают с настоящим PostgreSQL из docker-compose.
 */
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=warn"
})
@Sql(scripts = "/reset.sql")
@Import(ExperimentBase.Config.class)
public abstract class ExperimentBase {

    @Autowired
    protected JdbcTemplate jdbc;

    /** Явные границы транзакции без @Transactional на тесте. */
    @Autowired
    protected TransactionTemplate tx;

    @Autowired
    protected SqlCounter sql;

    protected long count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Long.class);
    }

    protected String titleOfBook(long id) {
        return jdbc.queryForObject("select title from books where id = ?", String.class, id);
    }

    protected static void step(String message) {
        System.out.printf("%n>>> %s%n", message);
    }

    @TestConfiguration
    static class Config {
        @Bean
        SqlCounter sqlCounter(EntityManagerFactory entityManagerFactory) {
            return new SqlCounter(entityManagerFactory);
        }
    }
}
