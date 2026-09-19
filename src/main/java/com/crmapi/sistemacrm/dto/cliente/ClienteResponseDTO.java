package com.crmapi.sistemacrm.dto.cliente;

import com.crmapi.sistemacrm.model.enums.StatusFunil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "ClienteResponse", description = "Cliente devolvido pela API")
public record ClienteResponseDTO(

        @Schema(description = "Identificador do cliente", example = "1")
        Long id,

        @Schema(description = "Nome do contato", example = "Mariana Alves Souza")
        String nome,

        @Schema(description = "E-mail do contato", example = "mariana.souza@techdominio.com.br")
        String email,

        @Schema(description = "Telefone do contato", example = "(11) 98877-6543")
        String telefone,

        @Schema(description = "CPF ou CNPJ", example = "412.559.870-11")
        String documento,

        @Schema(description = "Empresa do contato", example = "Tech Dominio Ltda")
        String empresa,

        @Schema(description = "Anotacoes sobre o atendimento",
                example = "Pediu proposta para 12 licencas.")
        String observacoes,

        @Schema(description = "Identificador do vendedor responsavel", example = "3")
        Long vendedorId,

        @Schema(description = "Nome do vendedor responsavel", example = "Ana Beatriz Moraes")
        String vendedorNome,

        @Schema(description = "Etapa atual no funil de vendas", example = "EM_CONTATO")
        StatusFunil statusFunil,

        @Schema(description = "Indica se o cliente esta ativo na carteira", example = "true")
        Boolean ativo,

        @Schema(description = "Data e hora do cadastro", example = "2026-09-05T09:00:00")
        LocalDateTime criadoEm,

        @Schema(description = "Data e hora da ultima alteracao", example = "2026-09-17T14:32:10")
        LocalDateTime atualizadoEm
) {
}
