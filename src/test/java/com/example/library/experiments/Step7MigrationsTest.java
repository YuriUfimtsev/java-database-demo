package com.example.library.experiments;

import com.example.library.domain.Book;
import com.example.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Эксперимент 7. Миграции Liquibase.
 * Liquibase выполняется при старте приложения, ещё до того как Hibernate проверит схему.
 */
class Step7MigrationsTest extends ExperimentBase {

    @Autowired
    BookRepository books;

    @Test
    void changelogHistory() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select orderexecuted, id, author, filename, md5sum, exectype
                from databasechangelog
                order by orderexecuted
                """);
        step("databasechangelog:");
        rows.forEach(System.out::println);
        assertThat(rows).extracting(r -> r.get("id")).contains("v1.1-add-isbn");
    }

    @Test
    void changelogLock() {
        Map<String, Object> lock = jdbc.queryForMap("select * from databasechangeloglock");
        step("databasechangeloglock: " + lock);
        assertThat(lock.get("locked")).isEqualTo(false);
    }

    @Test
    void newColumnWorks() {
        tx.executeWithoutResult(status -> books.findById(9L).orElseThrow().setIsbn("978-5-17-090335-2"));
        Book book = books.findByIsbn("978-5-17-090335-2").orElseThrow();
        assertThat(book.getTitle()).isEqualTo("Мастер и Маргарита");
    }
}
