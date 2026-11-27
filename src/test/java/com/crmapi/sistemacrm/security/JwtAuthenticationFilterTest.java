package com.crmapi.sistemacrm.security;

import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import com.crmapi.sistemacrm.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter: leitura do Bearer token")
class JwtAuthenticationFilterTest {

    private static final String EMAIL = "ana.moraes@crm.com";

    @Mock
    private JwtService jwtService;

    @Mock
    private UsuarioRepository usuarioRepository;

    private JwtAuthenticationFilter filtro;
    private MockHttpServletRequest request;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filtro = new JwtAuthenticationFilter(jwtService, usuarioRepository);
        request = new MockHttpServletRequest("GET", "/api/clientes");
        chain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private Usuario usuario(boolean ativo) {
        return Usuario.builder().id(3L).email(EMAIL).role(UsuarioRole.VENDEDOR).ativo(ativo).build();
    }

    private Authentication filtrar() throws Exception {
        filtro.doFilter(request, new MockHttpServletResponse(), chain);
        // A requisicao sempre segue adiante: quem decide o 401 e a cadeia de seguranca.
        assertThat(chain.getRequest()).isNotNull();
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    @DisplayName("autentica com o perfil do usuario quando o token e valido e a conta esta ativa")
    void deveAutenticarComTokenValido() throws Exception {
        // Arrange
        request.addHeader("Authorization", "Bearer token-bom");
        when(jwtService.extrairEmail("token-bom")).thenReturn(Optional.of(EMAIL));
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(true)));

        // Act
        Authentication autenticacao = filtrar();

        // Assert
        assertThat(autenticacao).isNotNull();
        assertThat(autenticacao.getPrincipal()).isEqualTo(EMAIL);
        assertThat(autenticacao.getAuthorities()).extracting("authority").containsExactly("ROLE_VENDEDOR");
    }

    @Test
    @DisplayName("nao autentica quando nao ha cabecalho Authorization")
    void naoDeveAutenticarSemCabecalho() throws Exception {
        // Act + Assert
        assertThat(filtrar()).isNull();
        verify(jwtService, never()).extrairEmail(anyString());
    }

    @Test
    @DisplayName("ignora cabecalho que nao e do tipo Bearer")
    void deveIgnorarCabecalhoQueNaoEBearer() throws Exception {
        // Arrange
        request.addHeader("Authorization", "Basic dXN1YXJpbzpzZW5oYQ==");

        // Act + Assert
        assertThat(filtrar()).isNull();
        verify(jwtService, never()).extrairEmail(anyString());
    }

    @Test
    @DisplayName("nao autentica quando o token e invalido ou expirado")
    void naoDeveAutenticarComTokenInvalido() throws Exception {
        // Arrange
        request.addHeader("Authorization", "Bearer token-ruim");
        when(jwtService.extrairEmail("token-ruim")).thenReturn(Optional.empty());

        // Act + Assert
        assertThat(filtrar()).isNull();
        verify(usuarioRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("nao autentica quando o dono do token foi removido")
    void naoDeveAutenticarUsuarioRemovido() throws Exception {
        // Arrange
        request.addHeader("Authorization", "Bearer token-bom");
        when(jwtService.extrairEmail("token-bom")).thenReturn(Optional.of(EMAIL));
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act + Assert
        assertThat(filtrar()).isNull();
    }

    @Test
    @DisplayName("corta o acesso na hora quando o usuario foi desativado, mesmo com token valido")
    void naoDeveAutenticarUsuarioInativo() throws Exception {
        // Arrange
        request.addHeader("Authorization", "Bearer token-bom");
        when(jwtService.extrairEmail("token-bom")).thenReturn(Optional.of(EMAIL));
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(false)));

        // Act + Assert
        assertThat(filtrar()).isNull();
    }

    @Test
    @DisplayName("mantem uma autenticacao que ja estava no contexto")
    void deveManterAutenticacaoExistente() throws Exception {
        // Arrange
        UsernamePasswordAuthenticationToken existente =
                new UsernamePasswordAuthenticationToken("outro@crm.com", null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(existente);
        request.addHeader("Authorization", "Bearer token-bom");

        // Act + Assert
        assertThat(filtrar()).isSameAs(existente);
        verify(jwtService, never()).extrairEmail(anyString());
    }
}
