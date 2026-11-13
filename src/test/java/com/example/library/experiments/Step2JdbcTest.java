package com.example.library.experiments;

import com.example.library.jdbc.BookRow;
import com.example.library.jdbc.JdbcBookDao;
import com.example.library.jdbc.JdbcLibraryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Эксперимент 2. JDBC, ResultSet и ручные транзакции. */
class Step2JdbcTest extends ExperimentBase {

    @Autowired
    JdbcBookDao dao;

    @Autowired
    JdbcLibraryService service;

    @Test
    void readBooksWithResultSet() throws Exception {
        List<BookRow> books = dao.findAll();
        books.forEach(System.out::println);
        assertThat(books).hasSize(10);
    }

    /** Прогноз: сколько авторов будет в базе после ошибки? */
    @Test
    void withoutTransactionAuthorStays() {
        assertThatThrownBy(() -> service.addAuthorWithBookAutoCommit("Иван Гончаров", "Обломов", true))
                .isInstanceOf(IllegalStateException.class);

        step("Авторов: " + count("authors") + ", книг: " + count("books"));
        assertThat(count("authors")).isEqualTo(5);   // автор без книг остался
        assertThat(count("books")).isEqualTo(10);
    }

    @Test
    void transactionRollsBack() {
        assertThatThrownBy(() -> service.addAuthorWithBook("Иван Гончаров", "Обломов", true))
                .isInstanceOf(IllegalStateException.class);

        step("Авторов: " + count("authors") + ", книг: " + count("books"));
        assertThat(count("authors")).isEqualTo(4);
        assertThat(count("books")).isEqualTo(10);
    }

    @Test
    void transactionCommits() throws Exception {
        service.addAuthorWithBook("Иван Гончаров", "Обломов", false);

        assertThat(count("authors")).isEqualTo(5);
        assertThat(count("books")).isEqualTo(11);
    }
}
