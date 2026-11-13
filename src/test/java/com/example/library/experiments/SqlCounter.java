package com.example.library.experiments;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

/**
 * Считает SQL-запросы, которые выполнил Hibernate.
 * Запросы через JdbcTemplate и чистый JDBC сюда не попадают.
 */
public class SqlCounter {

    private final Statistics statistics;

    public SqlCounter(EntityManagerFactory entityManagerFactory) {
        this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    public void reset() {
        statistics.clear();
    }

    public long count() {
        return statistics.getPrepareStatementCount();
    }

    public void print() {
        System.out.printf("%n>>> Hibernate выполнил SQL-запросов: %d%n%n", count());
    }
}
