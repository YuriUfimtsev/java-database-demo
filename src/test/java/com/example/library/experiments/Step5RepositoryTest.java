package com.example.library.experiments;

import com.example.library.domain.Author;
import com.example.library.domain.Book;
import com.example.library.repository.AuthorRepository;
import com.example.library.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Эксперимент 5. Методы JpaRepository и метод save(). */
class Step5RepositoryTest extends ExperimentBase {

    @Autowired
    BookRepository books;

    @Autowired
    AuthorRepository authors;

    @Test
    void derivedQueries() {
        List<Book> found = books.findByTitleContainingIgnoreCase("мир");
        step("Содержат 'мир': " + found);
        assertThat(found).extracting(Book::getTitle).containsExactly("Война и мир");

        assertThat(books.findByAuthorNameOrderByTitle("Антон Чехов"))
                .extracting(Book::getTitle).containsExactly("Вишнёвый сад", "Палата № 6");
        assertThat(books.countByAuthorId(1L)).isEqualTo(3);
        assertThat(books.existsByTitle("Идиот")).isTrue();
    }

    @Test
    void pagination() {
        Page<Book> page = books.findByAuthorName("Лев Толстой",
                PageRequest.of(0, 2, Sort.by("title")));   // страница 0, размер 2
        step("Страница: " + page.getContent() + ", всего " + page.getTotalElements());
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalPages()).isEqualTo(2);     // отдельный count-запрос
    }

    @Test
    void jpqlAndNative() {
        assertThat(books.searchByAuthor("толст")).hasSize(3);
        assertThat(books.searchNative("сердце")).extracting(Book::getTitle).containsExactly("Собачье сердце");
    }

    @Test
    void modifyingQuery() {
        int updated = tx.execute(status -> books.uppercaseTitles(3L));
        assertThat(updated).isEqualTo(2);
        assertThat(titleOfBook(7)).isEqualTo("ВИШНЁВЫЙ САД");
    }

    /** Новая сущность (id == null): save вызывает persist и возвращает тот же объект. */
    @Test
    void saveNewEntity() {
        Author author = new Author("Иван Тургенев");
        sql.reset();
        Author saved = authors.save(author);
        sql.print();
        assertThat(saved).isSameAs(author);
        assertThat(author.getId()).isEqualTo(5L);
    }

    /** Отсоединённая сущность (id != null): save вызывает merge и возвращает копию. */
    @Test
    void saveDetachedEntity() {
        Book detached = books.findById(1L).orElseThrow();   // после вызова уже detached
        detached.setTitle("Война и мир. Издание 2");

        sql.reset();
        Book saved = books.save(detached);
        sql.print();                                         // SELECT + UPDATE
        assertThat(saved).isNotSameAs(detached);
        assertThat(titleOfBook(1)).isEqualTo("Война и мир. Издание 2");
    }

    /** Управляемой сущности save не нужен: хватит dirty checking. */
    @Test
    void managedEntityNeedsNoSave() {
        tx.executeWithoutResult(status -> {
            Book book = books.findById(1L).orElseThrow();
            book.setTitle("Без save");
        });
        assertThat(titleOfBook(1)).isEqualTo("Без save");
    }
}
