package com.crmapi.sistemacrm.dto.usuario;

import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "UsuarioUpdate",
        description = "Dados cadastrais do integrante. A senha nao e alterada por este contrato")
public record UsuarioUpdateDTO(

        @Schema(description = "Nome do integrante", example = "Ana Beatriz Moraes", maxLength = 150)
        @NotBlank(message = "O nome e obrigatorio")
        @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
        String nome,

        @Schema(description = "E-mail de acesso, unico no sistema", example = "ana.moraes@crm.com", maxLength = 150)
        @NotBlank(message = "O email e obrigatorio")
        @Email(message = "O email informado e invalido")
        @Size(max = 150, message = "O email deve ter no maximo 150 caracteres")
        String email,

        @Schema(description = "Perfil de acesso do integrante", example = "GERENTE")
        @NotNull(message = "O role e obrigatorio")
        UsuarioRole role
) {
}
