package com.crmapi.sistemacrm.dto.usuario;

import com.crmapi.sistemacrm.model.enums.UsuarioRole;

import java.time.LocalDateTime;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        UsuarioRole role,
        Boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}
