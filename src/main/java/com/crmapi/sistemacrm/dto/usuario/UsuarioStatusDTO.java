package com.crmapi.sistemacrm.dto.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "UsuarioStatus", description = "Disponibilidade do integrante no sistema")
public record UsuarioStatusDTO(

        @Schema(description = "True reativa o acesso, false desativa", example = "false")
        @NotNull(message = "O campo ativo e obrigatorio")
        Boolean ativo
) {
}
