package com.example.library.service;

import com.example.library.domain.Author;
import com.example.library.domain.Book;
import com.example.library.repository.AuthorRepository;
import com.example.library.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Эксперимент 6. Тот же сценарий, что в JdbcLibraryService, но транзакциями управляет Spring.
 */
@Service
public class LibraryService {

    private final AuthorRepository authors;
    private final BookRepository books;
    private final AuditService audit;
    private final ValidationService validation;

    public LibraryService(AuthorRepository authors, BookRepository books,
                          AuditService audit, ValidationService validation) {
        this.authors = authors;
        this.books = books;
        this.audit = audit;
        this.validation = validation;
    }

    /** Одна транзакция на весь метод. RuntimeException откатывает всё. */
    @Transactional
    public Author addAuthorWithBooks(String name, List<String> titles, boolean failAfterFirstBook) {
        Author author = authors.save(new Author(name));
        for (String title : titles) {
            books.save(new Book(title, author));
            if (failAfterFirstBook) {
                throw new IllegalStateException("Сбой после первой книги");
            }
        }
        return author;
    }

    /** Тот же код без аннотации: каждый save() в своей транзакции. */
    public Author addAuthorWithBooksNoTx(String name, List<String> titles, boolean failAfterFirstBook) {
        Author author = authors.save(new Author(name));
        for (String title : titles) {
            books.save(new Book(title, author));
            if (failAfterFirstBook) {
                throw new IllegalStateException("Сбой после первой книги");
            }
        }
        return author;
    }

    /** Самовызов: this.addAuthorWithBooks() идёт мимо прокси, транзакции нет. */
    public Author importAuthor(String name) {
        return addAuthorWithBooks(name, List.of("Первая книга", "Вторая книга"), true);
    }

    /** Checked exception: по умолчанию транзакция фиксируется. */
    @Transactional
    public void addAuthorChecked(String name) throws LibraryException {
        authors.save(new Author(name));
        throw new LibraryException("Проверяемое исключение");
    }

    @Transactional(rollbackFor = LibraryException.class)
    public void addAuthorCheckedWithRollbackFor(String name) throws LibraryException {
        authors.save(new Author(name));
        throw new LibraryException("Проверяемое исключение");
    }

    /** Запись в журнал переживёт откат основной транзакции. */
    @Transactional
    public void addAuthorWithAudit(String name, boolean fail) {
        audit.log("Попытка добавить автора " + name);
        authors.save(new Author(name));
        if (fail) {
            throw new IllegalStateException("Сбой после записи в журнал");
        }
    }

    /** Поймали исключение из REQUIRED-метода, но транзакция уже помечена rollback-only. */
    @Transactional
    public void addAuthorIgnoringValidation(String name) {
        try {
            validation.validateName(name);
        } catch (IllegalArgumentException e) {
            System.out.println(">>> Ошибку проверки проглотили: " + e.getMessage());
        }
        authors.save(new Author(name));
    }

    @Transactional(readOnly = true)
    public long countBooksOf(String authorName) {
        return books.findByAuthorNameOrderByTitle(authorName).size();
    }
}
