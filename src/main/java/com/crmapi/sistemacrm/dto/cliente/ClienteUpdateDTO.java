package com.crmapi.sistemacrm.dto.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClienteUpdateDTO(

        @NotBlank(message = "O nome e obrigatorio")
        @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
        String nome,

        @NotBlank(message = "O email e obrigatorio")
        @Email(message = "O email informado e invalido")
        String email,

        String telefone,

        @NotNull(message = "O vendedorId e obrigatorio")
        Long vendedorId
) {
}
