package com.crmapi.sistemacrm.dto.usuario;

import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateDTO(

        @NotBlank(message = "O nome e obrigatorio")
        @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
        String nome,

        @NotBlank(message = "O email e obrigatorio")
        @Email(message = "O email informado e invalido")
        String email,

        @NotNull(message = "O role e obrigatorio")
        UsuarioRole role
) {
}
