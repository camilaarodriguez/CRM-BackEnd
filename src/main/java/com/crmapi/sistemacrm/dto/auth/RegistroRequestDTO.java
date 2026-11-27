package com.crmapi.sistemacrm.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cadastro publico de um novo usuario. Nao existe campo de perfil de proposito:
 * quem se cadastra sozinho entra sempre como VENDEDOR, e so a gestao promove.
 */
@Schema(name = "RegistroRequest", description = "Dados para criar uma conta no sistema")
public record RegistroRequestDTO(

        @Schema(description = "Nome completo", example = "Ana Beatriz Moraes", maxLength = 150)
        @NotBlank(message = "O nome e obrigatorio")
        @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
        String nome,

        @Schema(description = "E-mail de acesso, unico no sistema", example = "ana.moraes@crm.com", maxLength = 150)
        @NotBlank(message = "O email e obrigatorio")
        @Email(message = "O email informado e invalido")
        @Size(max = 150, message = "O email deve ter no maximo 150 caracteres")
        String email,

        @Schema(description = "Senha de acesso", example = "minhaSenha123", minLength = 6)
        @NotBlank(message = "A senha e obrigatoria")
        @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres")
        String senha
) {
}
