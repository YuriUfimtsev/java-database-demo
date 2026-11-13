package com.example.library.experiments;

import com.example.library.service.LibraryException;
import com.example.library.service.LibraryService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.UnexpectedRollbackException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Эксперимент 6. @Transactional.
 * Изначально: 4 автора, 10 книг. Для каждого теста сначала прогноз, потом запуск.
 */
class Step6TransactionalTest extends ExperimentBase {

    @Autowired
    LibraryService service;

    @Test
    void serviceIsProxy() {
        step("Класс бина: " + service.getClass().getName());
        assertThat(AopUtils.isAopProxy(service)).isTrue();
    }

    @Test
    void rollbackOnRuntimeException() {
        assertThatThrownBy(() -> service.addAuthorWithBooks("Иван Гончаров", List.of("Обломов", "Обрыв"), true))
                .isInstanceOf(IllegalStateException.class);
        printCounts();
        assertThat(count("authors")).isEqualTo(4);
        assertThat(count("books")).isEqualTo(10);
    }

    @Test
    void withoutTransaction() {
        assertThatThrownBy(() -> service.addAuthorWithBooksNoTx("Иван Гончаров", List.of("Обломов", "Обрыв"), true))
                .isInstanceOf(IllegalStateException.class);
        printCounts();
        assertThat(count("authors")).isEqualTo(5);
        assertThat(count("books")).isEqualTo(11);
    }

    @Test
    void selfInvocation() {
        assertThatThrownBy(() -> service.importAuthor("Иван Гончаров"))
                .isInstanceOf(IllegalStateException.class);
        printCounts();
        assertThat(count("authors")).isEqualTo(5);   // аннотация не сработала
        assertThat(count("books")).isEqualTo(11);
    }

    @Test
    void checkedExceptionCommits() {
        assertThatThrownBy(() -> service.addAuthorChecked("Иван Гончаров"))
                .isInstanceOf(LibraryException.class);
        printCounts();
        assertThat(count("authors")).isEqualTo(5);
    }

    @Test
    void rollbackForChecked() {
        assertThatThrownBy(() -> service.addAuthorCheckedWithRollbackFor("Иван Гончаров"))
                .isInstanceOf(LibraryException.class);
        printCounts();
        assertThat(count("authors")).isEqualTo(4);
    }

    @Test
    void requiresNewSurvivesRollback() {
        assertThatThrownBy(() -> service.addAuthorWithAudit("Иван Гончаров", true))
                .isInstanceOf(IllegalStateException.class);
        printCounts();
        assertThat(count("authors")).isEqualTo(4);
        assertThat(count("audit_log")).isEqualTo(1);
    }

    @Test
    void rollbackOnlyAfterCaughtException() {
        assertThatThrownBy(() -> service.addAuthorIgnoringValidation(" "))
                .isInstanceOf(UnexpectedRollbackException.class);
        printCounts();
        assertThat(count("authors")).isEqualTo(4);
    }

    private void printCounts() {
        step("Авторов: " + count("authors") + ", книг: " + count("books") + ", записей в журнале: " + count("audit_log"));
    }
}
