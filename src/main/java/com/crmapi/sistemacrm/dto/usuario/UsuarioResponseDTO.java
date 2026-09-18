package com.crmapi.sistemacrm.dto.usuario;

import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "UsuarioResponse", description = "Integrante da equipe devolvido pela API. A senha nunca e exposta")
public record UsuarioResponseDTO(

        @Schema(description = "Identificador do usuario", example = "3")
        Long id,

        @Schema(description = "Nome do integrante", example = "Ana Beatriz Moraes")
        String nome,

        @Schema(description = "E-mail de acesso", example = "ana.moraes@crm.com")
        String email,

        @Schema(description = "Perfil de acesso", example = "VENDEDOR")
        UsuarioRole role,

        @Schema(description = "Indica se o usuario esta ativo", example = "true")
        Boolean ativo,

        @Schema(description = "Data e hora do cadastro", example = "2026-05-21T09:00:00")
        LocalDateTime criadoEm,

        @Schema(description = "Data e hora da ultima alteracao", example = "2026-08-19T16:45:00")
        LocalDateTime atualizadoEm
) {
}
