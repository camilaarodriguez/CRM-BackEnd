package com.crmapi.sistemacrm.security;

import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import com.crmapi.sistemacrm.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioLogado: autor da requisicao atual")
class UsuarioLogadoTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioLogado usuarioLogado;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("devolve o usuario dono do token validado")
    void deveDevolverOUsuarioAutenticado() {
        // Arrange
        Usuario gerente = Usuario.builder().id(7L).email("gerente@crm.com").role(UsuarioRole.GERENTE).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("gerente@crm.com", null, List.of()));
        when(usuarioRepository.findByEmail("gerente@crm.com")).thenReturn(Optional.of(gerente));

        // Act + Assert
        assertThat(usuarioLogado.atual()).contains(gerente);
    }

    @Test
    @DisplayName("devolve vazio quando nao ha ninguem autenticado")
    void deveDevolverVazioSemAutenticacao() {
        // Act + Assert
        assertThat(usuarioLogado.atual()).isEmpty();
        verify(usuarioRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("ignora o usuario anonimo do Spring Security")
    void deveIgnorarUsuarioAnonimo() {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "chave", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        // Act + Assert
        assertThat(usuarioLogado.atual()).isEmpty();
        verify(usuarioRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("ignora uma autenticacao cujo principal nao e o e-mail")
    void deveIgnorarPrincipalQueNaoEEmail() {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(42L, null, List.of()));

        // Act + Assert
        assertThat(usuarioLogado.atual()).isEmpty();
        verify(usuarioRepository, never()).findByEmail(anyString());
    }
}
