package com.crmapi.sistemacrm.security;

import com.crmapi.sistemacrm.exception.ErrorResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Responde as recusas do Spring Security no mesmo formato JSON do
 * GlobalExceptionHandler, para que o front trate todo erro de um jeito so:
 *
 * - 401 quando a rota exige login e nao veio token, ou o token e invalido/expirado;
 * - 403 quando o usuario esta autenticado, mas o perfil dele nao tem permissao.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RespostaErroSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException excecao) throws IOException {
        escrever(request, response, HttpStatus.UNAUTHORIZED, "Nao autenticado",
                "Faca login para continuar: o token esta ausente, invalido ou expirado");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException excecao) throws IOException {
        escrever(request, response, HttpStatus.FORBIDDEN, "Acesso negado",
                "Seu perfil nao tem permissao para realizar esta operacao");
    }

    private void escrever(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                          String erro, String mensagem) throws IOException {
        log.warn("{} em {} {}", status.value(), request.getMethod(), request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                new ErrorResponseDTO(status.value(), erro, mensagem, request.getRequestURI()));
    }
}
