package com.crmapi.sistemacrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest", description = "Credenciais de acesso ao sistema")
public record LoginRequestDTO(

        @Schema(description = "E-mail cadastrado do usuario", example = "admin@crm.com")
        @NotBlank(message = "O email e obrigatorio")
        @Email(message = "O email informado e invalido")
        String email,

        @Schema(description = "Senha do usuario", example = "123456")
        @NotBlank(message = "A senha e obrigatoria")
        String senha
) {
}
