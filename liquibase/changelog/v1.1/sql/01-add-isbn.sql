alter table books
    add column isbn varchar(17) constraint books_isbn_key unique;
