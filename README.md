# Library: PostgreSQL в Java

Учебный проект к мастер-классу. Одно приложение, на котором по шагам разбираем
пул соединений, JDBC, JPA и Hibernate, Spring Data JPA, `@Transactional` и миграции Liquibase.

## Что нужно

- JDK 21
- Docker

Gradle ставить не нужно, в проекте есть Gradle Wrapper (`./gradlew`, в Windows `gradlew.bat`).
При первом запуске он сам скачает Gradle 8.14.3.

## Запуск

```bash
docker compose up -d          # PostgreSQL 16 на localhost:5432, база library
./gradlew bootRun             # Liquibase создаст схему и данные
```

В консоли должно появиться `База library: авторов 4, книг 10`.

Подключиться к базе можно так: `docker exec -it library-postgres psql -U library library`.
Подойдёт и любой клиент, адрес `localhost:5432`, пользователь и пароль `library`.

## Эксперименты

Каждый эксперимент это тестовый класс в `src/test/java/.../experiments`.
Перед каждым тестом база возвращается в исходное состояние (`reset.sql`).
Запускать удобнее из IDE по одному тесту и смотреть в консоль, там Hibernate печатает каждый SQL.

| Шаг | Класс | Что смотрим |
|---|---|---|
| 1 | `Step1ConnectionStringTest` | Строка подключения, параметры драйвера, `options` и `statement_timeout` |
| 1 | `Step1ConnectionPoolTest` | Стоимость соединения, ожидание свободного соединения, утечка |
| 2 | `Step2JdbcTest` | `ResultSet`, `commit` и `rollback` |
| 2 | `Step2SqlInjectionTest` | SQL-инъекции, `or '1'='1`, `union`, второй запрос, атака по времени, белый список |
| 3 | `Step3EntityManagerTest` | Контекст хранения, dirty checking, flush, detach и merge, инъекция в JPQL |
| 4 | `Step4FetchTypeTest` | `LAZY` и `EAGER`, N+1, `JOIN FETCH`, `@EntityGraph` |
| 5 | `Step5RepositoryTest` | Производные запросы, `@Query`, страницы, `save()` |
| 6 | `Step6TransactionalTest` | Откат, checked-исключения, самовызов, `REQUIRES_NEW`, rollback-only |
| 6 | `Step6RequiresNewPoolTest` | `REQUIRES_NEW` при пуле из одного соединения |
| 6 | `Step6IsolationTest` | Уровни изоляции, неповторяющееся чтение, фантомы, потерянное обновление, `isolation` в `@Transactional` |
| 7 | `Step7MigrationsTest` | `databasechangelog`, `databasechangeloglock`, новая колонка |

Один класс из консоли запускается так: `./gradlew test --tests '*Step4FetchTypeTest'`.
Вывод тестов (SQL и результаты экспериментов) печатается в консоль.

### Этапы в git

Каждый этап отмечен тегом. Если что-то сломалось, можно вернуться к началу этапа.

```bash
git tag                         # stage-0-start ... stage-7-migrations
git switch -c my-work stage-3-jpa
```

### Задание к шагу 4

В `Book.author` поменяйте `FetchType.LAZY` на `FetchType.EAGER` и снова запустите
`Step4FetchTypeTest`. Сравните число запросов в тестах `findAllWithoutTouchingAuthors` и `findById`.

### Задание к шагу 7

Добавьте колонку `published_year` типа `integer`, допускающую `null`.

1. Папка `liquibase/changelog/v1.2` с файлами `sql/01-add-published-year.sql`,
   `rollback/01-add-published-year.sql` и `changelog.xml`, где описан changeSet.
2. `include` новой версии в общем `liquibase/changelog/changelog.xml`.
3. Поле `Integer publishedYear` в `Book`.
4. Запустите приложение и посмотрите на `databasechangelog`.

Затем попробуйте изменить уже выполненный файл `v1.0/sql/01-create-tables.sql`
и перезапустить приложение. Почему оно не стартует?

Решение лежит в ветке `practice-solution`. Посмотреть изменения можно командой
`git diff main practice-solution`.

## Миграции

```
liquibase/
  changelog/
    changelog.xml       общий changelog, подключает версии по порядку
    v1.0/
      changelog.xml     changeSet этой версии
      sql/              SQL миграций
      rollback/         SQL отката
    v1.1/
  migrator-image/       пример образа мигратора, в проекте не используется
```

Папка `liquibase` подключена к ресурсам в `build.gradle.kts`, поэтому Spring Boot находит общий
changelog по пути `classpath:changelog/changelog.xml` и выполняет миграции при старте приложения.

## Полезные запросы

```sql
-- соединения пула со стороны PostgreSQL
select pid, application_name, state, query
from pg_stat_activity
where datname = 'library';

-- история миграций
select orderexecuted, id, author, filename, md5sum from databasechangelog;
select * from databasechangeloglock;
```

Если изменения в схеме зашли в тупик, проще всего пересоздать базу командой
`docker compose down -v && docker compose up -d`.
