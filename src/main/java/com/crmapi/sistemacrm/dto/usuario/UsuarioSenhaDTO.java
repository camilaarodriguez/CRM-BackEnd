package com.crmapi.sistemacrm.dto.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "UsuarioSenha", description = "Troca de senha do integrante")
public record UsuarioSenhaDTO(

        @Schema(description = "Senha atual, conferida contra o hash guardado", example = "123456")
        @NotBlank(message = "A senha atual e obrigatoria")
        String senhaAtual,

        @Schema(description = "Nova senha", example = "nova-senha-123", minLength = 6)
        @NotBlank(message = "A nova senha e obrigatoria")
        @Size(min = 6, message = "A nova senha deve ter no minimo 6 caracteres")
        String novaSenha
) {
}
