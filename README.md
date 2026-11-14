# library-demo

Spring Boot 3.5, PostgreSQL 16, HikariCP, Spring Data JPA, Liquibase.

## Требования

- JDK 21
- Docker

Сборка через Gradle Wrapper (`./gradlew`, в Windows `gradlew.bat`), Gradle 8.14.3 скачается при первом запуске.

## Запуск

```bash
docker compose up -d
./gradlew bootRun
```

PostgreSQL поднимается на `localhost:5432`, база, пользователь и пароль `library`.
При старте Liquibase создаёт схему и тестовые данные, в лог выводится `База library: авторов 4, книг 10`.

Консоль базы:

```bash
docker exec -it library-postgres psql -U library library
```

Пересоздать базу:

```bash
docker compose down -v && docker compose up -d
```

## Эксперименты

Тесты в `src/test/java/com/example/library/experiments`. Перед каждым тестом база
сбрасывается скриптом `reset.sql`. Hibernate логирует каждый SQL-запрос.

| Шаг | Класс | Тема |
|---|---|---|
| 1 | `Step1ConnectionStringTest` | Строка подключения, параметры драйвера, `statement_timeout` |
| 1 | `Step1ConnectionPoolTest` | Пул соединений, ожидание соединения, утечка |
| 2 | `Step2JdbcTest` | `ResultSet`, `commit`, `rollback` |
| 2 | `Step2SqlInjectionTest` | SQL-инъекции, `PreparedStatement`, белый список |
| 3 | `Step3EntityManagerTest` | Контекст хранения, dirty checking, flush, detach, merge |
| 4 | `Step4FetchTypeTest` | `LAZY`, `EAGER`, N+1, `JOIN FETCH`, `@EntityGraph` |
| 5 | `Step5RepositoryTest` | Производные запросы, `@Query`, пагинация, `save()` |
| 6 | `Step6TransactionalTest` | Правила отката, самовызов, `REQUIRES_NEW`, rollback-only |
| 6 | `Step6RequiresNewPoolTest` | `REQUIRES_NEW` при пуле из одного соединения |
| 6 | `Step6IsolationTest` | Уровни изоляции, аномалии чтения, потерянное обновление |
| 7 | `Step7MigrationsTest` | `databasechangelog`, `databasechangeloglock` |

Запуск одного класса:

```bash
./gradlew test --tests '*Step4FetchTypeTest'
```

## Этапы

Состояние проекта после каждого этапа отмечено тегом от `stage-0-start` до `stage-7-migrations`.

```bash
git switch -c my-work stage-3-jpa
```

## Задания

### Шаг 4

В `Book.author` замените `FetchType.LAZY` на `FetchType.EAGER` и запустите `Step4FetchTypeTest`.
Сравните число запросов в `findAllWithoutTouchingAuthors` и `findById`.

### Шаг 7

Добавьте в `books` колонку `published_year integer null`.

1. Создайте `liquibase/changelog/v1.2` с файлами `changelog.xml`,
   `sql/01-add-published-year.sql` и `rollback/01-add-published-year.sql`.
2. Подключите `v1.2/changelog.xml` в `liquibase/changelog/changelog.xml`.
3. Добавьте поле `Integer publishedYear` в `Book`.
4. Запустите приложение и проверьте `databasechangelog`.

После этого измените уже выполненный `v1.0/sql/01-create-tables.sql` и перезапустите приложение.
Объясните, почему оно не стартует.

Решение в ветке `practice-solution`:

```bash
git diff main practice-solution
```

## Миграции

```
liquibase/
  changelog/
    changelog.xml       подключает версии по порядку
    v1.0/
      changelog.xml
      sql/
      rollback/
    v1.1/
  migrator-image/       пример Dockerfile мигратора, в сборке не используется
```

Каталог `liquibase` добавлен в ресурсы в `build.gradle.kts`, changelog доступен как
`classpath:changelog/changelog.xml`. Миграции выполняются при старте приложения.

## Запросы

```sql
select pid, application_name, state, query
from pg_stat_activity
where datname = 'library';

select orderexecuted, id, author, filename, md5sum from databasechangelog;
select * from databasechangeloglock;
```
