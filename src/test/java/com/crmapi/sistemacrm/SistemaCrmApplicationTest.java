package com.crmapi.sistemacrm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mockStatic;

@DisplayName("SistemaCrmApplication")
class SistemaCrmApplicationTest {

    @Test
    @DisplayName("o main entrega a inicializacao ao Spring Boot")
    void mainDeveIniciarAAplicacaoSpring() {
        // Arrange: o SpringApplication e mockado para nao subir a aplicacao de verdade.
        String[] args = {};
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            // Act + Assert
            assertThatCode(() -> SistemaCrmApplication.main(args)).doesNotThrowAnyException();

            spring.verify(() -> SpringApplication.run(SistemaCrmApplication.class, args));
        }
    }
}
