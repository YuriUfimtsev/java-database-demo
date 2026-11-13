package com.example.library.experiments;

import com.example.library.domain.Author;
import com.example.library.domain.Book;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Эксперимент 3. EntityManager и контекст хранения (persistence context).
 * Смотрите в лог: строки org.hibernate.SQL показывают, когда реально уходит SQL.
 */
class Step3EntityManagerTest extends ExperimentBase {

    @Autowired
    EntityManager em;

    /** Прогноз: сколько SELECT выполнится? */
    @Test
    void firstLevelCache() {
        sql.reset();
        tx.executeWithoutResult(status -> {
            Book first = em.find(Book.class, 1L);
            Book second = em.find(Book.class, 1L);
            assertThat(first).isSameAs(second);   // один и тот же объект
        });
        sql.print();
        assertThat(sql.count()).isEqualTo(1);
    }

    /** Dirty checking: UPDATE без вызова save/update. */
    @Test
    void dirtyChecking() {
        tx.executeWithoutResult(status -> {
            Book book = em.find(Book.class, 1L);
            book.setTitle("Война и мир. Том 1");
            step("setTitle вызван, UPDATE пока не отправлен");
        });
        step("Транзакция зафиксирована");
        assertThat(titleOfBook(1)).isEqualTo("Война и мир. Том 1");
    }

    /** Flush перед JPQL-запросом: иначе запрос не увидел бы изменение. */
    @Test
    void autoFlushBeforeQuery() {
        tx.executeWithoutResult(status -> {
            Book book = em.find(Book.class, 1L);
            book.setTitle("Мир и война");
            step("Выполняем JPQL-запрос");
            Long count = em.createQuery("select count(b) from Book b where b.title = 'Мир и война'", Long.class)
                    .getSingleResult();
            assertThat(count).isEqualTo(1);  // UPDATE ушёл раньше SELECT
        });
    }

    /** IDENTITY: INSERT выполняется сразу при persist, чтобы узнать id. */
    @Test
    void persistWithIdentity() {
        tx.executeWithoutResult(status -> {
            Author author = new Author("Иван Бунин");
            step("До persist: id = " + author.getId());
            em.persist(author);
            step("После persist: id = " + author.getId());
            em.persist(new Book("Тёмные аллеи", author));
        });
        assertThat(count("authors")).isEqualTo(5);
    }

    /** Отсоединённая сущность не отслеживается. merge возвращает другой объект. */
    @Test
    void detachAndMerge() {
        Book detached = tx.execute(status -> em.find(Book.class, 1L));
        // транзакция закончилась, контекст закрыт, detached больше не управляется
        detached.setTitle("Новое название");
        assertThat(titleOfBook(1)).isEqualTo("Война и мир");     // в базе ничего не поменялось

        Book managed = tx.execute(status -> em.merge(detached));  // SELECT + UPDATE
        assertThat(managed).isNotSameAs(detached);
        assertThat(titleOfBook(1)).isEqualTo("Новое название");
    }

    /** JPQL тоже уязвим, если склеивать строки. Параметры спасают. */
    @Test
    void jpqlInjection() {
        String input = "x' or '1'='1";
        List<Book> unsafe = em.createQuery(
                "select b from Book b where b.title = '" + input + "'", Book.class).getResultList();
        List<Book> safe = em.createQuery(
                "select b from Book b where b.title = :title", Book.class)
                .setParameter("title", input).getResultList();
        step("Склейка: " + unsafe.size() + ", параметр: " + safe.size());
        assertThat(unsafe).hasSize(10);
        assertThat(safe).isEmpty();
    }
}
