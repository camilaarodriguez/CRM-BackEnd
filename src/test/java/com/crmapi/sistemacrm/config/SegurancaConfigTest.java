package com.crmapi.sistemacrm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato do codificador de senhas e da leitura das origens do CORS. Trocar o
 * encoder por um reversivel, ou por NoOpPasswordEncoder, derrubaria estes testes.
 * A cadeia de filtros em si e coberta em SegurancaConfigWebTest.
 */
@DisplayName("SegurancaConfig: codificador de senhas e CORS")
class SegurancaConfigTest {

    private final PasswordEncoder encoder = new SegurancaConfig().passwordEncoder();

    @Test
    @DisplayName("publica um codificador BCrypt")
    void devePublicarUmCodificadorBCrypt() {
        // Assert
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    @DisplayName("gera um hash que nao revela a senha original")
    void naoDeveGuardarASenhaEmTextoPuro() {
        // Act
        String hash = encoder.encode("123456");

        // Assert
        assertThat(hash).isNotEqualTo("123456").doesNotContain("123456").startsWith("$2a$");
    }

    @Test
    @DisplayName("reconhece a senha correta e recusa a errada")
    void deveConferirASenhaPeloHash() {
        // Arrange
        String hash = encoder.encode("123456");

        // Act + Assert
        assertThat(encoder.matches("123456", hash)).isTrue();
        assertThat(encoder.matches("123455", hash)).isFalse();
    }

    @Test
    @DisplayName("gera hashes diferentes para a mesma senha")
    void deveUsarUmSalDiferenteACadaHash() {
        // Act
        String primeiro = encoder.encode("123456");
        String segundo = encoder.encode("123456");

        // Assert: com sal aleatorio, duas pessoas com a mesma senha ficam com
        // hashes distintos, e um vazamento do banco nao denuncia senhas repetidas.
        assertThat(primeiro).isNotEqualTo(segundo);
        assertThat(encoder.matches("123456", primeiro)).isTrue();
        assertThat(encoder.matches("123456", segundo)).isTrue();
    }

    @Test
    @DisplayName("le as origens do CORS separadas por virgula, ignorando espacos e itens vazios")
    void deveLerAsOrigensDoCors() {
        // Act
        CorsConfigurationSource fonte = new SegurancaConfig()
                .corsConfigurationSource(" http://localhost:4200 , ,http://3.85.10.20 ");
        CorsConfiguration config = fonte.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/clientes"));

        // Assert
        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:4200", "http://3.85.10.20");
        assertThat(config.getAllowedHeaders()).contains("Authorization", "Content-Type");
    }
}
