package com.example.library.service;

/** Проверяемое (checked) исключение: по умолчанию НЕ вызывает rollback. */
public class LibraryException extends Exception {

    public LibraryException(String message) {
        super(message);
    }
}
