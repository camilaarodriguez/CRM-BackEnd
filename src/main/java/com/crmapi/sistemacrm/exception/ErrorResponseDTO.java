package com.crmapi.sistemacrm.exception;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "ErrorResponse", description = "Formato unico de erro devolvido pelo GlobalExceptionHandler")
public record ErrorResponseDTO(

        @Schema(description = "Momento em que o erro ocorreu", example = "2026-09-17T18:05:00")
        LocalDateTime timestamp,

        @Schema(description = "Codigo HTTP da resposta", example = "404")
        int status,

        @Schema(description = "Categoria do erro", example = "Recurso nao encontrado")
        String erro,

        @Schema(description = "Descricao do que aconteceu", example = "Cliente nao encontrado com o id: 999")
        String mensagem,

        @Schema(description = "Caminho da requisicao que falhou", example = "/api/clientes/999")
        String caminho,

        @ArraySchema(
                arraySchema = @Schema(description = "Campos invalidos, preenchido nas falhas de validacao"),
                schema = @Schema(example = "O nome e obrigatorio"))
        List<String> detalhes
) {
    public ErrorResponseDTO(int status, String erro, String mensagem, String caminho) {
        this(LocalDateTime.now(), status, erro, mensagem, caminho, List.of());
    }

    public ErrorResponseDTO(int status, String erro, String mensagem, String caminho, List<String> detalhes) {
        this(LocalDateTime.now(), status, erro, mensagem, caminho, detalhes);
    }
}
