package com.crmapi.sistemacrm.security;

import com.crmapi.sistemacrm.model.Usuario;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Emite e confere os tokens JWT da aplicacao.
 *
 * O token carrega o e-mail no "sub", o id e o perfil como claims e uma data de
 * expiracao. A assinatura e HMAC-SHA256 com a chave de JWT_SECRET: sem ela
 * ninguem consegue forjar ou alterar um token sem que a validacao perceba.
 */
@Slf4j
@Service
public class JwtService {

    /** HMAC-SHA256 exige uma chave de pelo menos 256 bits. */
    static final int TAMANHO_MINIMO_DA_CHAVE = 32;

    private final SecretKey chave;
    private final long expiracaoMs;

    public JwtService(@Value("${jwt.secret}") String segredo,
                      @Value("${jwt.expiration-ms}") long expiracaoMs) {
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_DA_CHAVE) {
            // Falhar na subida e melhor do que rodar com uma chave fraca ou vazia.
            throw new IllegalStateException(
                    "JWT_SECRET precisa ter pelo menos " + TAMANHO_MINIMO_DA_CHAVE + " caracteres");
        }
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMs = expiracaoMs;
    }

    public TokenGerado gerar(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plusMillis(expiracaoMs);

        String token = Jwts.builder()
                .subject(usuario.getEmail())
                .claim("id", usuario.getId())
                .claim("role", usuario.getRole().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiraEm))
                .signWith(chave)
                .compact();

        return new TokenGerado(token, expiraEm);
    }

    /**
     * Devolve o e-mail do dono do token, ou vazio quando o token esta expirado,
     * adulterado, assinado com outra chave ou simplesmente mal formado.
     */
    public Optional<String> extrairEmail(String token) {
        try {
            String email = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Optional.ofNullable(email);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token JWT recusado: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public record TokenGerado(String token, Instant expiraEm) {
    }
}
