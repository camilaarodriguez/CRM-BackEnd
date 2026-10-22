package com.crmapi.sistemacrm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Codificador de senhas da aplicacao.
 *
 * O BCrypt gera um sal aleatorio por senha, entao duas pessoas com a mesma
 * senha ficam com hashes diferentes no banco, e e deliberadamente lento, o que
 * torna a quebra por forca bruta cara.
 */
@Configuration
public class SegurancaConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
