package com.crmapi.sistemacrm.security;

import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("JwtService: emissao e validacao de tokens")
class JwtServiceTest {

    private static final String SEGREDO = "chave-de-teste-com-pelo-menos-32-caracteres-para-o-hmac";
    private static final long UMA_HORA = 3_600_000L;

    private final JwtService jwtService = new JwtService(SEGREDO, UMA_HORA);

    private final Usuario usuario = Usuario.builder()
            .id(7L)
            .email("gerente@crm.com")
            .role(UsuarioRole.GERENTE)
            .build();

    @Nested
    @DisplayName("Emissao")
    class Emissao {

        @Test
        @DisplayName("gera um token com e-mail, id, perfil e a expiracao configurada")
        void deveGerarTokenComAsClaims() {
            // Act
            JwtService.TokenGerado gerado = jwtService.gerar(usuario);

            // Assert
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(gerado.token())
                    .getPayload();
            assertThat(claims.getSubject()).isEqualTo("gerente@crm.com");
            assertThat(claims.get("id", Long.class)).isEqualTo(7L);
            assertThat(claims.get("role", String.class)).isEqualTo("GERENTE");
            assertThat(gerado.expiraEm()).isCloseTo(Instant.now().plusMillis(UMA_HORA), within(5, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("recusa subir com uma chave menor que 256 bits")
        void deveRecusarChaveCurta() {
            // Act + Assert
            assertThatThrownBy(() -> new JwtService("curta", UMA_HORA))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT_SECRET");
        }

        @Test
        @DisplayName("recusa subir sem chave nenhuma")
        void deveRecusarChaveNula() {
            // Act + Assert
            assertThatThrownBy(() -> new JwtService(null, UMA_HORA))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Validacao")
    class Validacao {

        @Test
        @DisplayName("extrai o e-mail de um token valido")
        void deveExtrairOEmailDeTokenValido() {
            // Arrange
            String token = jwtService.gerar(usuario).token();

            // Act + Assert
            assertThat(jwtService.extrairEmail(token)).contains("gerente@crm.com");
        }

        @Test
        @DisplayName("recusa um token expirado")
        void deveRecusarTokenExpirado() {
            // Arrange: expiracao negativa gera um token que ja nasce vencido.
            String vencido = new JwtService(SEGREDO, -1_000L).gerar(usuario).token();

            // Act + Assert
            assertThat(jwtService.extrairEmail(vencido)).isEmpty();
        }

        @Test
        @DisplayName("recusa um token assinado com outra chave")
        void deveRecusarTokenDeOutraChave() {
            // Arrange
            String deOutraChave = new JwtService("outra-chave-qualquer-com-mais-de-32-caracteres", UMA_HORA)
                    .gerar(usuario).token();

            // Act + Assert
            assertThat(jwtService.extrairEmail(deOutraChave)).isEmpty();
        }

        @Test
        @DisplayName("recusa um token com o conteudo adulterado")
        void deveRecusarTokenAdulterado() {
            // Arrange: mantem a assinatura original, mas troca o conteudo para se passar por ADMIN.
            String[] partes = jwtService.gerar(usuario).token().split("\\.");
            String conteudoFalso = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"sub\":\"admin@crm.com\",\"role\":\"ADMIN\"}".getBytes(StandardCharsets.UTF_8));
            String adulterado = partes[0] + "." + conteudoFalso + "." + partes[2];

            // Act + Assert
            assertThat(jwtService.extrairEmail(adulterado)).isEmpty();
        }

        @Test
        @DisplayName("recusa texto que nao e um JWT e token vazio")
        void deveRecusarLixo() {
            // Act + Assert
            assertThat(jwtService.extrairEmail("isto-nao-e-um-jwt")).isEmpty();
            assertThat(jwtService.extrairEmail("")).isEmpty();
        }
    }
}
