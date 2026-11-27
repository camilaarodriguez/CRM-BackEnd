package com.crmapi.sistemacrm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenApiConfig: metadados do Swagger")
class OpenApiConfigTest {

    private final OpenAPI openApi = new OpenApiConfig().openApiDoSistemaCrm();

    @Test
    @DisplayName("publica titulo, licenca e servidor relativo da documentacao")
    void deveConfigurarMetadadosDaDocumentacao() {
        // Assert
        assertThat(openApi.getInfo().getTitle()).isEqualTo("API do Sistema CRM");
        assertThat(openApi.getInfo().getLicense().getName()).isEqualTo("MIT");
        assertThat(openApi.getServers()).extracting("url").containsExactly("/");
    }

    @Test
    @DisplayName("declara o esquema Bearer JWT e o exige por padrao")
    void deveDeclararOEsquemaJwt() {
        // Act
        SecurityScheme esquema = openApi.getComponents().getSecuritySchemes().get(OpenApiConfig.ESQUEMA_JWT);

        // Assert
        assertThat(esquema.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(esquema.getScheme()).isEqualTo("bearer");
        assertThat(esquema.getBearerFormat()).isEqualTo("JWT");
        assertThat(openApi.getSecurity()).hasSize(1);
        assertThat(openApi.getSecurity().get(0)).containsKey(OpenApiConfig.ESQUEMA_JWT);
    }
}
