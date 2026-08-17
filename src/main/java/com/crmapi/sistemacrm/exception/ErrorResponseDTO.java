package com.crmapi.sistemacrm.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponseDTO(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<String> detalhes
) {
    public ErrorResponseDTO(int status, String erro, String mensagem, String caminho) {
        this(LocalDateTime.now(), status, erro, mensagem, caminho, List.of());
    }

    public ErrorResponseDTO(int status, String erro, String mensagem, String caminho, List<String> detalhes) {
        this(LocalDateTime.now(), status, erro, mensagem, caminho, detalhes);
    }
}
