package com.crmapi.sistemacrm.service.impl;

import com.crmapi.sistemacrm.dto.auth.LoginRequestDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioResponseDTO;
import com.crmapi.sistemacrm.exception.BusinessException;
import com.crmapi.sistemacrm.mapper.UsuarioMapper;
import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.repository.UsuarioRepository;
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

    @Override
    public UsuarioResponseDTO autenticar(LoginRequestDTO dto) {
        Optional<Usuario> encontrado = usuarioRepository.findByEmail(dto.email());

        // A mesma mensagem para e-mail inexistente e senha errada: dizer qual dos
        // dois falhou entregaria a um atacante quais e-mails estao cadastrados.
        if (encontrado.isEmpty() || !passwordEncoder.matches(dto.senha(), encontrado.get().getSenha())) {
            log.warn("Tentativa de login malsucedida para o email: {}", dto.email());
            throw new BusinessException("E-mail ou senha invalidos");
        }

        Usuario usuario = encontrado.get();
        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            log.warn("Tentativa de login de usuario inativo: id={}", usuario.getId());
            throw new BusinessException("Este usuario esta inativo. Procure um administrador.");
        }

        log.info("Login efetuado: id={}, role={}", usuario.getId(), usuario.getRole());
        return usuarioMapper.paraResponseDTO(usuario);
    }
}
