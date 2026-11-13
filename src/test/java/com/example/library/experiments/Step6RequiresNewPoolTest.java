package com.example.library.experiments;

import com.example.library.service.LibraryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Связь транзакций и пула: REQUIRES_NEW нужно второе соединение.
 * В пуле одно соединение, и его держит внешняя транзакция.
 * Прогноз: что произойдёт?
 */
@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.datasource.hikari.connection-timeout=3000"
})
class Step6RequiresNewPoolTest extends ExperimentBase {

    @Autowired
    LibraryService service;

    @Test
    void requiresNewWithSingleConnection() {
        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> service.addAuthorWithAudit("Иван Гончаров", false))
                .isInstanceOf(RuntimeException.class)
                .satisfies(e -> step("Через " + (System.currentTimeMillis() - start) + " мс: " + e));
    }
}
