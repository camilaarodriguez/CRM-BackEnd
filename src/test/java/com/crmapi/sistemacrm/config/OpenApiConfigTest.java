package com.crmapi.sistemacrm.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenApiConfig: metadados do Swagger")
class OpenApiConfigTest {

    @Test
    @DisplayName("publica titulo, licenca e servidor da documentacao")
    void deveConfigurarMetadadosDaDocumentacao() {
        // Act
        OpenAPI openApi = new OpenApiConfig().openApiDoSistemaCrm();

        // Assert
        assertThat(openApi.getInfo().getTitle()).isEqualTo("API do Sistema CRM");
        assertThat(openApi.getInfo().getLicense().getName()).isEqualTo("MIT");
        assertThat(openApi.getServers()).extracting("url").containsExactly("http://localhost:8080");
    }
}
