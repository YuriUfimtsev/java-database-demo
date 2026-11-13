plugins {
    java
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "1.0.0"
description = "Мастер-класс по PostgreSQL в Java. JDBC, JPA, Spring Data JPA, транзакции и миграции"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// Папка liquibase становится ресурсом: Spring найдёт classpath:changelog/changelog.xml.
// Dockerfile мигратора в ресурсы не нужен.
sourceSets {
    main {
        resources {
            srcDir("liquibase")
            exclude("migrator-image/**")
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Data JPA, Hibernate ORM, HikariCP, Spring JDBC
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Миграции схемы
    implementation("org.liquibase:liquibase-core")

    // JDBC-драйвер PostgreSQL
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Печатать вывод тестов в консоль, там SQL Hibernate и результаты экспериментов
    testLogging {
        showStandardStreams = true
        events("passed", "failed", "skipped")
    }
}
