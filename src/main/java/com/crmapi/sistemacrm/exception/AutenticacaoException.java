package com.crmapi.sistemacrm.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Falha na entrada do usuario no sistema. Cada subclasse define o status HTTP
 * coerente com o motivo da recusa, e o GlobalExceptionHandler apenas o repassa.
 */
@Getter
public abstract class AutenticacaoException extends RuntimeException {

    private final HttpStatus status;
    private final String titulo;

    protected AutenticacaoException(HttpStatus status, String titulo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.titulo = titulo;
    }
}
