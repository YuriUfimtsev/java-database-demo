package com.example.library.repository;

import com.example.library.domain.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    // 1. Производные запросы: SQL строится по имени метода
    List<Book> findByTitleContainingIgnoreCase(String part);

    List<Book> findByAuthorNameOrderByTitle(String authorName);   // путь author.name, будет join

    long countByAuthorId(Long authorId);

    Optional<Book> findByIsbn(String isbn);

    boolean existsByTitle(String title);

    Page<Book> findByAuthorName(String authorName, Pageable pageable);

    // 2. JPQL: запрос к сущностям, а не к таблицам
    @Query("select b from Book b where lower(b.author.name) like lower(concat('%', :name, '%'))")
    List<Book> searchByAuthor(@Param("name") String name);

    // 3. Нативный SQL: возможности конкретной СУБД
    @Query(value = "select * from books where title ilike '%' || :q || '%' order by id", nativeQuery = true)
    List<Book> searchNative(@Param("q") String q);

    // 4. Изменяющий запрос: идёт в базу мимо контекста хранения
    @Modifying(clearAutomatically = true)
    @Query("update Book b set b.title = upper(b.title) where b.author.id = :authorId")
    int uppercaseTitles(@Param("authorId") Long authorId);

    // Решение N+1, вариант 1: JOIN FETCH в JPQL
    @Query("select b from Book b join fetch b.author order by b.id")
    List<Book> findAllWithAuthor();

    // Решение N+1, вариант 2: граф сущностей поверх производного запроса
    @EntityGraph(attributePaths = "author")
    List<Book> findAllByOrderByIdAsc();
}
