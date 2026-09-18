package com.crmapi.sistemacrm.dto.usuario;

import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "UsuarioCreate", description = "Dados para cadastrar um integrante da equipe")
public record UsuarioCreateDTO(

        @Schema(description = "Nome do integrante", example = "Ana Beatriz Moraes", maxLength = 150)
        @NotBlank(message = "O nome e obrigatorio")
        @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
        String nome,

        @Schema(description = "E-mail de acesso, unico no sistema", example = "ana.moraes@crm.com", maxLength = 150)
        @NotBlank(message = "O email e obrigatorio")
        @Email(message = "O email informado e invalido")
        @Size(max = 150, message = "O email deve ter no maximo 150 caracteres")
        String email,

        @Schema(description = "Senha de acesso", example = "123456", minLength = 6)
        @NotBlank(message = "A senha e obrigatoria")
        @Size(min = 6, message = "A senha deve ter no minimo 6 caracteres")
        String senha,

        @Schema(description = "Perfil de acesso do integrante", example = "VENDEDOR")
        @NotNull(message = "O role e obrigatorio")
        UsuarioRole role
) {
}
