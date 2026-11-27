package com.crmapi.sistemacrm.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados da documentacao exposta pelo Swagger UI em /swagger-ui.html.
 */
@Configuration
public class OpenApiConfig {

    static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI openApiDoSistemaCrm() {
        return new OpenAPI()
                .info(new Info()
                        .title("API do Sistema CRM")
                        .version("1.0.0")
                        .description("""
                                API REST do CRM: gestao de usuarios, carteira de clientes, funil de vendas,
                                distribuicao de leads entre vendedores e central de conversas.

                                Padroes adotados:
                                - Autenticacao por JWT: faca POST /api/auth/login, copie o token e use o
                                  botao Authorize. Sem token as rotas protegidas respondem 401, e sem o
                                  perfil necessario respondem 403.
                                - Respostas paginadas no formato Page do Spring Data (parametros page, size e sort).
                                - Erros padronizados pelo GlobalExceptionHandler, com timestamp, status, erro,
                                  mensagem, caminho e a lista de detalhes nas falhas de validacao.
                                - Entidades nunca sao expostas: a entrada e a saida usam DTOs dedicados.
                                """)
                        .contact(new Contact().name("Equipe Sistema CRM"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                // URL relativa: o Swagger chama a API no mesmo endereco em que foi aberto,
                // seja localhost:8080 em desenvolvimento ou o IP publico da EC2 atras do Nginx.
                .servers(List.of(new Server().url("/").description("Servidor atual")));
    }
}
