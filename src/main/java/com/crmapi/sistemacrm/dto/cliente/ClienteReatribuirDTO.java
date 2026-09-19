package com.crmapi.sistemacrm.dto.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "ClienteReatribuir", description = "Transferencia do cliente para outro vendedor")
public record ClienteReatribuirDTO(

        @Schema(description = "Identificador do vendedor que assume o cliente", example = "4")
        @NotNull(message = "O novoVendedorId e obrigatorio")
        Long novoVendedorId,

        @Schema(description = "Motivo da transferencia, guardado no historico de atribuicoes",
                example = "Equilibrio de carteira entre os vendedores", maxLength = 255)
        @Size(max = 255, message = "O motivo deve ter no maximo 255 caracteres")
        String motivo
) {
}
