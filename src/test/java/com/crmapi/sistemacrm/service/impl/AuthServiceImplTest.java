package com.crmapi.sistemacrm.service.impl;

import com.crmapi.sistemacrm.dto.auth.LoginRequestDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioResponseDTO;
import com.crmapi.sistemacrm.exception.BusinessException;
import com.crmapi.sistemacrm.mapper.UsuarioMapper;
import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import com.crmapi.sistemacrm.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl: entrada no sistema com e-mail e senha")
class AuthServiceImplTest {

    private static final String EMAIL = "ana.moraes@crm.com";
    private static final String SENHA_DIGITADA = "123456";
    private static final String HASH_GUARDADO = "$2a$10$hashBCryptDeExemploParaOTeste";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        // O mapper nao tem dependencia nenhuma, entao entra inteiro: o que esta sob
        // teste e a regra de autenticacao, nao a conversao para DTO.
        authService = new AuthServiceImpl(usuarioRepository, new UsuarioMapper(), passwordEncoder);
    }

    private Usuario usuario(boolean ativo) {
        return Usuario.builder()
                .id(3L)
                .nome("Ana Beatriz Moraes")
                .email(EMAIL)
                .senha(HASH_GUARDADO)
                .role(UsuarioRole.VENDEDOR)
                .ativo(ativo)
                .build();
    }

    @Nested
    @DisplayName("Quando as credenciais conferem")
    class CredenciaisValidas {

        @Test
        @DisplayName("devolve os dados do usuario autenticado")
        void deveAutenticarEDevolverOsDadosDoUsuario() {
            // Arrange
            when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(true)));
            when(passwordEncoder.matches(SENHA_DIGITADA, HASH_GUARDADO)).thenReturn(true);

            // Act
            UsuarioResponseDTO autenticado = authService.autenticar(new LoginRequestDTO(EMAIL, SENHA_DIGITADA));

            // Assert
            assertThat(autenticado.id()).isEqualTo(3L);
            assertThat(autenticado.nome()).isEqualTo("Ana Beatriz Moraes");
            assertThat(autenticado.email()).isEqualTo(EMAIL);
            assertThat(autenticado.role()).isEqualTo(UsuarioRole.VENDEDOR);
            assertThat(autenticado.ativo()).isTrue();
        }

        @Test
        @DisplayName("confere a senha digitada contra o hash guardado, nunca por igualdade de texto")
        void deveCompararASenhaPeloEncoder() {
            // Arrange
            when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(true)));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            // Act
            authService.autenticar(new LoginRequestDTO(EMAIL, SENHA_DIGITADA));

            // Assert
            ArgumentCaptor<String> digitada = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> guardado = ArgumentCaptor.forClass(String.class);
            verify(passwordEncoder).matches(digitada.capture(), guardado.capture());
            assertThat(digitada.getValue()).isEqualTo(SENHA_DIGITADA);
            assertThat(guardado.getValue()).isEqualTo(HASH_GUARDADO);
        }
    }

    @Nested
    @DisplayName("Quando as credenciais nao conferem")
    class CredenciaisInvalidas {

        @Test
        @DisplayName("recusa o login quando o e-mail nao esta cadastrado")
        void deveRecusarEmailInexistente() {
            // Arrange
            when(usuarioRepository.findByEmail("ninguem@crm.com")).thenReturn(Optional.empty());

            LoginRequestDTO credenciais = new LoginRequestDTO("ninguem@crm.com", SENHA_DIGITADA);

            // Act + Assert
            assertThatThrownBy(() -> authService.autenticar(credenciais))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("E-mail ou senha invalidos");

            // Sem usuario nao ha hash para comparar: o encoder nem chega a ser chamado.
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("recusa o login quando a senha esta errada")
        void deveRecusarSenhaErrada() {
            // Arrange
            when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(true)));
            when(passwordEncoder.matches("senha-errada", HASH_GUARDADO)).thenReturn(false);
            LoginRequestDTO credenciais = new LoginRequestDTO(EMAIL, "senha-errada");

            // Act + Assert
            assertThatThrownBy(() -> authService.autenticar(credenciais))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("E-mail ou senha invalidos");
        }

        @Test
        @DisplayName("responde a mesma coisa para e-mail inexistente e para senha errada")
        void naoDeveRevelarQuaisEmailsEstaoCadastrados() {
            // Arrange
            when(usuarioRepository.findByEmail("ninguem@crm.com")).thenReturn(Optional.empty());
            when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(true)));
            when(passwordEncoder.matches("senha-errada", HASH_GUARDADO)).thenReturn(false);

            LoginRequestDTO desconhecido = new LoginRequestDTO("ninguem@crm.com", SENHA_DIGITADA);
            LoginRequestDTO senhaTrocada = new LoginRequestDTO(EMAIL, "senha-errada");

            // Act
            Throwable emailInexistente = catchThrowable(() -> authService.autenticar(desconhecido));
            Throwable senhaErrada = catchThrowable(() -> authService.autenticar(senhaTrocada));

            // Assert: respostas diferentes entregariam a um atacante quais e-mails existem.
            assertThat(emailInexistente).hasSameClassAs(senhaErrada);
            assertThat(emailInexistente.getMessage()).isEqualTo(senhaErrada.getMessage());
        }
    }

    @Nested
    @DisplayName("Quando o usuario esta inativo")
    class UsuarioInativo {

        @Test
        @DisplayName("barra a entrada mesmo com a senha correta")
        void deveBarrarUsuarioInativo() {
            // Arrange
            when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(false)));
            when(passwordEncoder.matches(SENHA_DIGITADA, HASH_GUARDADO)).thenReturn(true);
            LoginRequestDTO credenciais = new LoginRequestDTO(EMAIL, SENHA_DIGITADA);

            // Act + Assert
            assertThatThrownBy(() -> authService.autenticar(credenciais))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Este usuario esta inativo. Procure um administrador.");
        }

        @Test
        @DisplayName("explica que o impedimento e o cadastro, e nao a senha")
        void deveDiferenciarInativoDeCredencialInvalida() {
            // Arrange
            when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(false)));
            when(passwordEncoder.matches(SENHA_DIGITADA, HASH_GUARDADO)).thenReturn(true);

            LoginRequestDTO credenciais = new LoginRequestDTO(EMAIL, SENHA_DIGITADA);

            // Act
            Throwable erro = catchThrowable(() -> authService.autenticar(credenciais));

            // Assert: quem acertou a senha ja provou ser dono da conta, entao aqui a
            // mensagem especifica orienta sem vazar nada.
            assertThat(erro).hasMessageContaining("inativo");
            assertThat(erro.getMessage()).isNotEqualTo("E-mail ou senha invalidos");
        }
    }
}
