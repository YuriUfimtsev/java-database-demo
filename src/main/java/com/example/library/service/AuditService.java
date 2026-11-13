package com.example.library.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final JdbcTemplate jdbc;

    public AuditService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Отдельная транзакция: запись в журнале останется, даже если внешняя откатится.
     * Внешняя транзакция приостанавливается, но держит своё соединение,
     * поэтому здесь из пула берётся второе соединение.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String message) {
        jdbc.update("insert into audit_log (message) values (?)", message);
    }
}
