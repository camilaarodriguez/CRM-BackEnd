package com.crmapi.sistemacrm.security;

import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.repository.UsuarioRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Quem esta fazendo a requisicao atual, segundo o JWT validado pelo
 * JwtAuthenticationFilter. Usado para registrar o autor das acoes, como quem
 * reatribuiu um cliente ou quem enviou uma mensagem.
 */
@Component
@RequiredArgsConstructor
public class UsuarioLogado {

    private final UsuarioRepository usuarioRepository;

    /** O usuario autenticado, ou vazio quando nao ha login (por exemplo, numa tarefa interna). */
    public Optional<Usuario> atual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof String email) {
            return usuarioRepository.findByEmail(email);
        }
        return Optional.empty();
    }
}
