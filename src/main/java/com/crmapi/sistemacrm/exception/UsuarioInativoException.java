package com.crmapi.sistemacrm.exception;

import org.springframework.http.HttpStatus;

/**
 * Senha correta, mas o cadastro esta desativado: 403 Forbidden. A pessoa provou
 * ser dona da conta, so nao tem permissao de entrar no momento.
 */
public class UsuarioInativoException extends AutenticacaoException {

    public UsuarioInativoException() {
        super(HttpStatus.FORBIDDEN, "Acesso bloqueado", "Este usuario esta inativo. Procure um administrador.");
    }
}
