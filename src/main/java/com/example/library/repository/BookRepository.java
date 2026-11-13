package com.example.library.repository;

import com.example.library.domain.Book;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    // Решение N+1, вариант 1: JOIN FETCH в JPQL
    @Query("select b from Book b join fetch b.author order by b.id")
    List<Book> findAllWithAuthor();

    // Решение N+1, вариант 2: граф сущностей поверх производного запроса
    @EntityGraph(attributePaths = "author")
    List<Book> findAllByOrderByIdAsc();
}
