package com.crmapi.sistemacrm.service.impl;

import com.crmapi.sistemacrm.dto.auth.LoginRequestDTO;
import com.crmapi.sistemacrm.dto.auth.LoginResponseDTO;
import com.crmapi.sistemacrm.dto.auth.RegistroRequestDTO;
import com.crmapi.sistemacrm.exception.BusinessException;
import com.crmapi.sistemacrm.exception.CredenciaisInvalidasException;
import com.crmapi.sistemacrm.exception.UsuarioInativoException;
import com.crmapi.sistemacrm.mapper.UsuarioMapper;
import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import com.crmapi.sistemacrm.repository.UsuarioRepository;
import com.crmapi.sistemacrm.security.JwtService;
import com.crmapi.sistemacrm.service.AuthService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public LoginResponseDTO autenticar(LoginRequestDTO dto) {
        Optional<Usuario> encontrado = usuarioRepository.findByEmail(dto.email());

        // A mesma mensagem para e-mail inexistente e senha errada: dizer qual dos
        // dois falhou entregaria a um atacante quais e-mails estao cadastrados.
        if (encontrado.isEmpty() || !passwordEncoder.matches(dto.senha(), encontrado.get().getSenha())) {
            log.warn("Tentativa de login malsucedida para o email: {}", dto.email());
            throw new CredenciaisInvalidasException();
        }

        Usuario usuario = encontrado.get();
        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            log.warn("Tentativa de login de usuario inativo: id={}", usuario.getId());
            throw new UsuarioInativoException();
        }

        log.info("Login efetuado: id={}, role={}", usuario.getId(), usuario.getRole());
        return emitirToken(usuario);
    }

    @Override
    @Transactional
    public LoginResponseDTO registrar(RegistroRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            log.warn("Tentativa de cadastro com email ja existente: {}", dto.email());
            throw new BusinessException("Ja existe um usuario cadastrado com o email informado");
        }

        Usuario novo = Usuario.builder()
                .nome(dto.nome())
                .email(dto.email())
                .senha(passwordEncoder.encode(dto.senha()))
                // O perfil nunca vem da requisicao: ninguem se cadastra como ADMIN.
                .role(UsuarioRole.VENDEDOR)
                .ativo(true)
                .build();

        Usuario salvo = usuarioRepository.save(novo);
        log.info("Novo usuario cadastrado pelo formulario publico: id={}", salvo.getId());
        return emitirToken(salvo);
    }

    private LoginResponseDTO emitirToken(Usuario usuario) {
        JwtService.TokenGerado token = jwtService.gerar(usuario);
        return new LoginResponseDTO(token.token(), token.expiraEm(), usuarioMapper.paraResponseDTO(usuario));
    }
}
