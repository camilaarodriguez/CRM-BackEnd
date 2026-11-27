package com.crmapi.sistemacrm.config;

import com.crmapi.sistemacrm.repository.UsuarioRepository;
import com.crmapi.sistemacrm.security.JwtAuthenticationFilter;
import com.crmapi.sistemacrm.security.JwtService;
import com.crmapi.sistemacrm.security.RespostaErroSeguranca;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Seguranca da API.
 *
 * - Stateless: nao ha sessao no servidor, cada requisicao traz o proprio JWT.
 * - Publicas: login, cadastro, documentacao do Swagger e o preflight do CORS.
 * - Gestao de equipe e reatribuicao de carteira: apenas ADMIN e GERENTE.
 * - Todo o resto exige um usuario autenticado.
 *
 * O BCrypt gera um sal aleatorio por senha, entao duas pessoas com a mesma
 * senha ficam com hashes diferentes no banco, e e deliberadamente lento, o que
 * torna a quebra por forca bruta cara.
 */
@Configuration
@EnableWebSecurity
public class SegurancaConfig {

    private static final String[] PERFIS_DE_GESTAO = {"ADMIN", "GERENTE"};

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http,
                                                 JwtService jwtService,
                                                 UsuarioRepository usuarioRepository,
                                                 RespostaErroSeguranca respostaErro) throws Exception {
        return http
                // CSRF protege sessoes baseadas em cookie; aqui o token vai no cabecalho.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Listar e consultar a equipe e liberado a todos: os formularios de
                        // cliente e os filtros de conversa precisam da lista de vendedores.
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/usuarios/*/senha").authenticated()
                        .requestMatchers("/api/usuarios/**").hasAnyRole(PERFIS_DE_GESTAO)
                        .requestMatchers(HttpMethod.PATCH, "/api/clientes/*/reatribuir").hasAnyRole(PERFIS_DE_GESTAO)
                        .anyRequest().authenticated())
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(respostaErro)
                        .accessDeniedHandler(respostaErro))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, usuarioRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Origens vindas de CORS_ORIGINS (separadas por virgula). Em producao o front e
     * servido pelo mesmo Nginx da API, mas a politica fica declarada para o
     * desenvolvimento local e para quem acessar a API a partir de outra origem.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${cors.allowed-origins}") String origens) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origens.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Location"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", config);
        return fonte;
    }
}
