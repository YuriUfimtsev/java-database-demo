# Пример образа мигратора. В учебном проекте он не используется:
# миграции запускает Spring Boot при старте приложения.
# На проде такой образ запускают отдельным шагом до выкладки приложения.
#
# Сборка из корня проекта:
#   docker build -f liquibase/migrator-image/Dockerfile -t library-migrator liquibase
#
# Адрес базы, логин и пароль передаются переменными окружения
# LIQUIBASE_COMMAND_URL, LIQUIBASE_COMMAND_USERNAME, LIQUIBASE_COMMAND_PASSWORD
FROM liquibase/liquibase:4.31.1

COPY changelog /liquibase/changelog

CMD ["--changelog-file=changelog/changelog.xml", "update"]
