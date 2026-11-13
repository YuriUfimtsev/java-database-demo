package com.example.library.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Какой уровень изоляции на самом деле получает транзакция.
 * JdbcTemplate внутри @Transactional работает на том же соединении, что и транзакция.
 */
@Service
public class IsolationService {

    private final JdbcTemplate jdbc;

    public IsolationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Isolation.DEFAULT: уровень берётся у СУБД. */
    @Transactional(readOnly = true)
    public String defaultLevel() {
        return jdbc.queryForObject("show transaction_isolation", String.class);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public String repeatableRead() {
        return jdbc.queryForObject("show transaction_isolation", String.class);
    }
}
