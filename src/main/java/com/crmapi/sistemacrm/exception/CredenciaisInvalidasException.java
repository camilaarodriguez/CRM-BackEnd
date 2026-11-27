package com.crmapi.sistemacrm.exception;

import org.springframework.http.HttpStatus;

/** E-mail inexistente ou senha errada: 401 Unauthorized. */
public class CredenciaisInvalidasException extends AutenticacaoException {

    public CredenciaisInvalidasException() {
        super(HttpStatus.UNAUTHORIZED, "Credenciais invalidas", "E-mail ou senha invalidos");
    }
}
