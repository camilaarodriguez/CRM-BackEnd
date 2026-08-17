package com.crmapi.sistemacrm.dto.usuario;

import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioCreateDTO(

        @NotBlank(message = "O nome e obrigatorio")
        @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
        String nome,

        @NotBlank(message = "O email e obrigatorio")
        @Email(message = "O email informado e invalido")
        String email,

        @NotBlank(message = "A senha e obrigatoria")
        @Size(min = 6, message = "A senha deve ter no minimo 6 caracteres")
        String senha,

        @NotNull(message = "O role e obrigatorio")
        UsuarioRole role
) {
}
