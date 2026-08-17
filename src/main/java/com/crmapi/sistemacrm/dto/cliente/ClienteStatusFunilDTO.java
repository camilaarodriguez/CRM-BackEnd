package com.crmapi.sistemacrm.dto.cliente;

import com.crmapi.sistemacrm.model.enums.StatusFunil;
import jakarta.validation.constraints.NotNull;

public record ClienteStatusFunilDTO(

        @NotNull(message = "O statusFunil e obrigatorio")
        StatusFunil statusFunil
) {
}
