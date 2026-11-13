package com.example.library.experiments;

import com.example.library.domain.Author;
import com.example.library.domain.Book;
import com.example.library.repository.AuthorRepository;
import com.example.library.repository.BookRepository;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Эксперимент 4. FetchType и проблема N+1.
 * В базе 10 книг и 4 автора. Перед запуском каждого теста сделайте прогноз числа запросов.
 *
 * Затем поменяйте в Book.author fetch на EAGER и запустите тесты снова.
 */
class Step4FetchTypeTest extends ExperimentBase {

    @Autowired
    BookRepository books;

    @Autowired
    AuthorRepository authors;

    /** LAZY: 1 запрос за книгами + по одному на каждого автора. */
    @Test
    void nPlusOne() {
        sql.reset();
        tx.executeWithoutResult(status -> {
            List<Book> all = books.findAll();
            step("Книги загружены, обращаемся к авторам");
            all.forEach(b -> System.out.println(b.getTitle() + " / " + b.getAuthor().getName()));
        });
        sql.print();   // LAZY: 5 = 1 + 4 (повторный автор берётся из контекста)
    }

    /** EAGER не обязательно значит JOIN: для findAll Hibernate всё равно сделает N+1. */
    @Test
    void findAllWithoutTouchingAuthors() {
        sql.reset();
        tx.executeWithoutResult(status -> books.findAll());
        sql.print();   // LAZY: 1. EAGER: 5, хотя к авторам мы даже не обращались
    }

    /** find по id: при EAGER Hibernate сделает left join сразу. */
    @Test
    void findById() {
        sql.reset();
        tx.executeWithoutResult(status -> {
            Book book = books.findById(1L).orElseThrow();
            step("id автора без запроса: " + book.getAuthor().getId());
            step("Имя автора: " + book.getAuthor().getName());
        });
        sql.print();   // LAZY: 2. EAGER: 1
    }

    @Test
    void joinFetch() {
        sql.reset();
        tx.executeWithoutResult(status ->
                books.findAllWithAuthor().forEach(b -> b.getAuthor().getName()));
        sql.print();
        assertThat(sql.count()).isEqualTo(1);
    }

    @Test
    void entityGraph() {
        sql.reset();
        tx.executeWithoutResult(status ->
                books.findAllByOrderByIdAsc().forEach(b -> b.getAuthor().getName()));
        sql.print();
        assertThat(sql.count()).isEqualTo(1);
    }

    /** N+1 для коллекции: у @OneToMany LAZY по умолчанию. */
    @Test
    void collectionNPlusOne() {
        sql.reset();
        tx.executeWithoutResult(status ->
                authors.findAll().forEach(a -> System.out.println(a.getName() + ": " + a.getBooks().size())));
        sql.print();   // 5 = 1 + 4

        sql.reset();
        tx.executeWithoutResult(status ->
                authors.findAllWithBooks().forEach(a -> System.out.println(a.getName() + ": " + a.getBooks().size())));
        sql.print();   // 1
    }

    /** LAZY вне транзакции: контекст уже закрыт, загрузить автора нечем. */
    @Test
    void lazyOutsideTransaction() {
        Book book = books.findById(1L).orElseThrow();   // транзакция только внутри findById
        Author author = book.getAuthor();                // прокси, а не настоящий Author
        step("Класс автора: " + author.getClass().getSimpleName());
        assertThatThrownBy(author::getName).isInstanceOf(LazyInitializationException.class);
    }
}
