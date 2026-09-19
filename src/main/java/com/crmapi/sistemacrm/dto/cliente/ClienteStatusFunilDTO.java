package com.crmapi.sistemacrm.dto.cliente;

import com.crmapi.sistemacrm.model.enums.StatusFunil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "ClienteStatusFunil", description = "Nova etapa do cliente no funil de vendas")
public record ClienteStatusFunilDTO(

        @Schema(description = "Etapa de destino no funil", example = "NEGOCIACAO")
        @NotNull(message = "O statusFunil e obrigatorio")
        StatusFunil statusFunil
) {
}
