package com.crmapi.sistemacrm.dto.cliente;

import com.crmapi.sistemacrm.model.enums.StatusFunil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "ClienteCreate", description = "Dados para cadastrar um cliente na carteira")
public record ClienteCreateDTO(

        @Schema(description = "Nome do contato", example = "Mariana Alves Souza", maxLength = 150)
        @NotBlank(message = "O nome e obrigatorio")
        @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
        String nome,

        @Schema(description = "E-mail do contato", example = "mariana.souza@techdominio.com.br", maxLength = 150)
        @Email(message = "O email informado e invalido")
        @Size(max = 150, message = "O email deve ter no maximo 150 caracteres")
        String email,

        @Schema(description = "Telefone do contato, unico por cliente", example = "(11) 98877-6543", maxLength = 20)
        @NotBlank(message = "O telefone e obrigatorio")
        @Size(max = 20, message = "O telefone deve ter no maximo 20 caracteres")
        String telefone,

        @Schema(description = "CPF ou CNPJ, unico quando informado", example = "412.559.870-11", maxLength = 18)
        @Size(max = 18, message = "O documento deve ter no maximo 18 caracteres")
        String documento,

        @Schema(description = "Empresa do contato", example = "Tech Dominio Ltda", maxLength = 150)
        @Size(max = 150, message = "A empresa deve ter no maximo 150 caracteres")
        String empresa,

        @Schema(description = "Anotacoes livres sobre o atendimento",
                example = "Chegou pelo anuncio do Instagram. Pediu proposta para 12 licencas.")
        String observacoes,

        @Schema(description = "Identificador do vendedor que assume o cliente", example = "3")
        @NotNull(message = "O vendedorId e obrigatorio")
        Long vendedorId,

        @Schema(description = "Etapa inicial no funil. Quando omitido, o cliente entra como NOVO")
        StatusFunil statusFunil
) {
}
