package com.crmapi.sistemacrm.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> tratarNaoEncontrado(ResourceNotFoundException ex,
                                                                  HttpServletRequest request) {
        ErrorResponseDTO erro = new ErrorResponseDTO(
                HttpStatus.NOT_FOUND.value(),
                "Recurso nao encontrado",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDTO> tratarRegraDeNegocio(BusinessException ex,
                                                                   HttpServletRequest request) {
        ErrorResponseDTO erro = new ErrorResponseDTO(
                HttpStatus.CONFLICT.value(),
                "Violacao de regra de negocio",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

    /**
     * Rede de seguranca para violacoes de restricao do banco, como telefone ou
     * documento duplicado, que antes chegavam ao cliente como erro interno.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> tratarViolacaoDeIntegridade(DataIntegrityViolationException ex,
                                                                        HttpServletRequest request) {
        ErrorResponseDTO erro = new ErrorResponseDTO(
                HttpStatus.CONFLICT.value(),
                "Violacao de integridade",
                "A operacao conflita com um registro existente ou com um vinculo obrigatorio",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> tratarValidacao(MethodArgumentNotValidException ex,
                                                               HttpServletRequest request) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        ErrorResponseDTO erro = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Erro de validacao",
                "Um ou mais campos sao invalidos",
                request.getRequestURI(),
                detalhes
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    /**
     * Corpo que o Jackson nao consegue ler: JSON quebrado, campo com tipo trocado
     * ou enum desconhecido. E erro de quem chamou, entao 400 - antes caia no
     * tratador generico e voltava como 500.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> tratarCorpoIlegivel(HttpMessageNotReadableException ex,
                                                                HttpServletRequest request) {
        log.warn("Corpo ilegivel em {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponseDTO erro = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Corpo da requisicao invalido",
                "O corpo enviado nao pode ser lido como JSON valido",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    /**
     * Parametro de rota ou de consulta com o tipo errado, como /api/clientes/abc
     * ou um status de funil inexistente. Tambem e 400, e nao 500.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> tratarParametroComTipoErrado(MethodArgumentTypeMismatchException ex,
                                                                         HttpServletRequest request) {
        String detalhe = "O valor informado para o parametro '" + ex.getName() + "' nao e valido";
        log.warn("Parametro invalido em {}: {}", request.getRequestURI(), detalhe);
        ErrorResponseDTO erro = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Parametro invalido",
                detalhe,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> tratarArgumentoInvalido(IllegalArgumentException ex,
                                                                       HttpServletRequest request) {
        ErrorResponseDTO erro = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Requisicao invalida",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> tratarErroGenerico(Exception ex, HttpServletRequest request) {
        // O cliente recebe uma mensagem generica, mas o rastro completo fica no log:
        // sem isto um 500 nao deixaria nenhuma pista de diagnostico.
        log.error("Erro nao tratado em {}", request.getRequestURI(), ex);
        ErrorResponseDTO erro = new ErrorResponseDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro interno",
                "Ocorreu um erro inesperado ao processar a requisicao",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }
}
