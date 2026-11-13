package com.example.library.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValidationService {

    /** REQUIRED: участвует во внешней транзакции. Исключение помечает её rollback-only. */
    @Transactional
    public void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Пустое имя автора");
        }
    }
}
