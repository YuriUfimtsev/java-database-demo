package com.example.library.repository;

import com.example.library.domain.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    Optional<Author> findByName(String name);

    // Коллекция подгружается тем же запросом: решение N+1 для @OneToMany
    @Query("select a from Author a left join fetch a.books order by a.id")
    List<Author> findAllWithBooks();
}
