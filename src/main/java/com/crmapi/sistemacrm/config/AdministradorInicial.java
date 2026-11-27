package com.crmapi.sistemacrm.config;

import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import com.crmapi.sistemacrm.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o primeiro administrador quando o banco ainda nao tem nenhum.
 *
 * As migracoes so criam as tabelas, entao numa instalacao nova ninguem conseguiria
 * acessar a gestao de equipe. Os dados vem de ADMIN_EMAIL e ADMIN_PASSWORD, gerados
 * pelo script de deploy; sem eles, nada e criado. Se ja existe um ADMIN, o runner
 * nao faz nada, entao reiniciar a aplicacao nunca duplica nem sobrescreve contas.
 */
@Slf4j
@Component
public class AdministradorInicial implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String nome;
    private final String email;
    private final String senha;

    public AdministradorInicial(UsuarioRepository usuarioRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${admin.nome:Administrador}") String nome,
                                @Value("${admin.email:}") String email,
                                @Value("${admin.senha:}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || senha.isBlank()) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD nao definidos: administrador inicial nao sera criado");
            return;
        }
        if (usuarioRepository.existsByRole(UsuarioRole.ADMIN)) {
            log.info("Ja existe um administrador cadastrado: nada a fazer");
            return;
        }
        if (usuarioRepository.existsByEmail(email)) {
            log.warn("O e-mail {} ja pertence a outro usuario: administrador inicial nao criado", email);
            return;
        }

        usuarioRepository.save(Usuario.builder()
                .nome(nome)
                .email(email)
                .senha(passwordEncoder.encode(senha))
                .role(UsuarioRole.ADMIN)
                .ativo(true)
                .build());
        log.info("Administrador inicial criado: {}", email);
    }
}
