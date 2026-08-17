package com.crmapi.sistemacrm.dto.usuario;

import jakarta.validation.constraints.NotNull;

public record UsuarioStatusDTO(

        @NotNull(message = "O campo ativo e obrigatorio")
        Boolean ativo
) {
}
