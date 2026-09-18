package com.crmapi.sistemacrm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados da documentacao exposta pelo Swagger UI em /swagger-ui.html.
 */
@Configuration
public class OpenApiConfig {

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
                                - Respostas paginadas no formato Page do Spring Data (parametros page, size e sort).
                                - Erros padronizados pelo GlobalExceptionHandler, com timestamp, status, erro,
                                  mensagem, caminho e a lista de detalhes nas falhas de validacao.
                                - Entidades nunca sao expostas: a entrada e a saida usam DTOs dedicados.
                                """)
                        .contact(new Contact().name("Equipe Sistema CRM"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Ambiente local")));
    }
}
