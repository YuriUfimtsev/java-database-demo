insert into authors (name)
values ('Лев Толстой'),
       ('Фёдор Достоевский'),
       ('Антон Чехов'),
       ('Михаил Булгаков');

insert into books (title, author_id)
values ('Война и мир', (select id from authors where name = 'Лев Толстой')),
       ('Анна Каренина', (select id from authors where name = 'Лев Толстой')),
       ('Воскресение', (select id from authors where name = 'Лев Толстой')),
       ('Преступление и наказание', (select id from authors where name = 'Фёдор Достоевский')),
       ('Идиот', (select id from authors where name = 'Фёдор Достоевский')),
       ('Братья Карамазовы', (select id from authors where name = 'Фёдор Достоевский')),
       ('Вишнёвый сад', (select id from authors where name = 'Антон Чехов')),
       ('Палата № 6', (select id from authors where name = 'Антон Чехов')),
       ('Мастер и Маргарита', (select id from authors where name = 'Михаил Булгаков')),
       ('Собачье сердце', (select id from authors where name = 'Михаил Булгаков'));
