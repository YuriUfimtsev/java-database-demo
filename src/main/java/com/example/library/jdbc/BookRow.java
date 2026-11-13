package com.example.library.jdbc;

/** Строка результата запроса. Никакой магии ORM: просто данные из ResultSet. */
public record BookRow(long id, String title, String authorName) {
}
