package com.crmapi.sistemacrm.dto.auth;

import com.crmapi.sistemacrm.dto.usuario.UsuarioResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = "LoginResponse", description = "Token de acesso e dados do usuario autenticado")
public record LoginResponseDTO(

        @Schema(description = "JWT a ser enviado no cabecalho Authorization", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,

        @Schema(description = "Tipo do token", example = "Bearer")
        String tipo,

        @Schema(description = "Momento em que o token deixa de valer (UTC)", example = "2026-11-26T20:00:00Z")
        Instant expiraEm,

        @Schema(description = "Dados do usuario autenticado, sem a senha")
        UsuarioResponseDTO usuario
) {
    public static final String TIPO_BEARER = "Bearer";

    public LoginResponseDTO(String token, Instant expiraEm, UsuarioResponseDTO usuario) {
        this(token, TIPO_BEARER, expiraEm, usuario);
    }
}
