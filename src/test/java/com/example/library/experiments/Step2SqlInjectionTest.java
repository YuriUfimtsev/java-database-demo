package com.example.library.experiments;

import com.example.library.jdbc.BookRow;
import com.example.library.jdbc.JdbcBookDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Эксперимент 2б. SQL-инъекции.
 *
 * findByTitleUnsafe склеивает запрос из строк:
 *   ... where b.title = '<ввод пользователя>'
 * Перед каждым тестом: что вернёт этот ввод?
 */
class Step2SqlInjectionTest extends ExperimentBase {

    @Autowired
    JdbcBookDao dao;

    /** Условие, которое всегда истинно. */
    @Test
    void alwaysTrueCondition() throws Exception {
        List<BookRow> rows = dao.findByTitleUnsafe("x' or '1'='1");
        step("Найдено книг: " + rows.size());
        assertThat(rows).hasSize(10);
    }

    /** UNION: данные, которые запрос вообще не должен был читать. */
    @Test
    void unionReadsOtherData() throws Exception {
        List<BookRow> rows = dao.findByTitleUnsafe("x' union select 0, current_user, version() --");
        rows.forEach(System.out::println);
        assertThat(rows.get(0).authorName()).contains("PostgreSQL");
    }

    /** Второй запрос через точку с запятой. Исключение будет, но UPDATE уже выполнен. */
    @Test
    void stackedQueryChangesData() {
        assertThatThrownBy(() ->
                dao.findByTitleUnsafe("x'; update books set title = 'взломано' where id = 1; --"))
                .isInstanceOf(SQLException.class);
        step("Название книги 1: " + titleOfBook(1));
        assertThat(titleOfBook(1)).isEqualTo("взломано");
    }

    /** Атака по времени: результат не виден, но ответ сервера задерживается. */
    @Test
    void timeBasedInjection() throws Exception {
        long start = System.currentTimeMillis();
        dao.findByTitleUnsafe("x' union select 0, 'a', 'b' from pg_sleep(2) --");
        long ms = System.currentTimeMillis() - start;
        step("Запрос выполнялся " + ms + " мс");
        assertThat(ms).isGreaterThanOrEqualTo(2000);
    }

    /** Те же строки через PreparedStatement: это просто необычные названия книг. */
    @Test
    void preparedStatementIsSafe() throws Exception {
        List<String> inputs = List.of(
                "x' or '1'='1",
                "x' union select 0, current_user, version() --",
                "x'; update books set title = 'взломано' where id = 1; --");
        for (String input : inputs) {
            assertThat(dao.findByTitle(input)).isEmpty();
        }
        assertThat(titleOfBook(1)).isEqualTo("Война и мир");
    }

    /** Имя колонки параметром не передать: только белый список. */
    @Test
    void identifiersFromWhitelist() throws Exception {
        assertThat(dao.findAllSorted("title").get(0).title()).isEqualTo("Анна Каренина");
        assertThatThrownBy(() -> dao.findAllSorted("title; drop table books"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
