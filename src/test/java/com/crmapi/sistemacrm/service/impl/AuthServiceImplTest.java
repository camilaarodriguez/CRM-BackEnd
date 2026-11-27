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
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl: login e cadastro com emissao de JWT")
class AuthServiceImplTest {

    private static final String EMAIL = "ana.moraes@crm.com";
    private static final String SENHA_DIGITADA = "123456";
    private static final String HASH_GUARDADO = "$2a$10$hashBCryptDeExemploParaOTeste";
    private static final Instant EXPIRA_EM = Instant.parse("2026-11-26T20:00:00Z");

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        // O mapper nao tem dependencia nenhuma, entao entra inteiro: o que esta sob
        // teste e a regra de autenticacao, nao a conversao para DTO.
        authService = new AuthServiceImpl(usuarioRepository, new UsuarioMapper(), passwordEncoder, jwtService);
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

    private void tokenEmitido() {
        when(jwtService.gerar(any(Usuario.class))).thenReturn(new JwtService.TokenGerado("jwt-de-teste", EXPIRA_EM));
    }

    @Nested
    @DisplayName("Quando as credenciais conferem")
    class CredenciaisValidas {

        @Test
        @DisplayName("devolve o token, a expiracao e os dados do usuario autenticado")
        void deveAutenticarEDevolverOToken() {
            // Arrange
            when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(true)));
            when(passwordEncoder.matches(SENHA_DIGITADA, HASH_GUARDADO)).thenReturn(true);
            tokenEmitido();

            // Act
            LoginResponseDTO resposta = authService.autenticar(new LoginRequestDTO(EMAIL, SENHA_DIGITADA));

            // Assert
            assertThat(resposta.token()).isEqualTo("jwt-de-teste");
            assertThat(resposta.tipo()).isEqualTo("Bearer");
            assertThat(resposta.expiraEm()).isEqualTo(EXPIRA_EM);
            assertThat(resposta.usuario().id()).isEqualTo(3L);
            assertThat(resposta.usuario().email()).isEqualTo(EMAIL);
            assertThat(resposta.usuario().role()).isEqualTo(UsuarioRole.VENDEDOR);
        }

        @Test
        @DisplayName("confere a senha digitada contra o hash guardado, nunca por igualdade de texto")
        void deveCompararASenhaPeloEncoder() {
            // Arrange
            when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(true)));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            tokenEmitido();

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
                    .isInstanceOf(CredenciaisInvalidasException.class)
                    .hasMessage("E-mail ou senha invalidos");

            // Sem usuario nao ha hash para comparar: o encoder nem chega a ser chamado.
            verify(passwordEncoder, never()).matches(any(), any());
            verify(jwtService, never()).gerar(any());
        }

        @Test
        @DisplayName("recusa o login quando a senha esta errada e nao emite token")
        void deveRecusarSenhaErrada() {
            // Arrange
            when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(true)));
            when(passwordEncoder.matches("senha-errada", HASH_GUARDADO)).thenReturn(false);
            LoginRequestDTO credenciais = new LoginRequestDTO(EMAIL, "senha-errada");

            // Act + Assert
            assertThatThrownBy(() -> authService.autenticar(credenciais))
                    .isInstanceOf(CredenciaisInvalidasException.class)
                    .hasMessage("E-mail ou senha invalidos");
            verify(jwtService, never()).gerar(any());
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
        @DisplayName("barra a entrada mesmo com a senha correta e nao emite token")
        void deveBarrarUsuarioInativo() {
            // Arrange
            when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(false)));
            when(passwordEncoder.matches(SENHA_DIGITADA, HASH_GUARDADO)).thenReturn(true);
            LoginRequestDTO credenciais = new LoginRequestDTO(EMAIL, SENHA_DIGITADA);

            // Act + Assert
            assertThatThrownBy(() -> authService.autenticar(credenciais))
                    .isInstanceOf(UsuarioInativoException.class)
                    .hasMessage("Este usuario esta inativo. Procure um administrador.");
            verify(jwtService, never()).gerar(any());
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

    @Nested
    @DisplayName("Cadastro publico")
    class Registro {

        private final RegistroRequestDTO cadastro = new RegistroRequestDTO("Joao Pedro", "joao@crm.com", "segredo1");

        @Test
        @DisplayName("cria a conta como VENDEDOR ativo, com a senha em hash, e ja devolve o token")
        void deveCadastrarComoVendedorEDevolverToken() {
            // Arrange
            when(usuarioRepository.existsByEmail("joao@crm.com")).thenReturn(false);
            when(passwordEncoder.encode("segredo1")).thenReturn("$2a$10$hashDoJoao");
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
                Usuario salvo = inv.getArgument(0);
                salvo.setId(9L);
                return salvo;
            });
            tokenEmitido();

            // Act
            LoginResponseDTO resposta = authService.registrar(cadastro);

            // Assert
            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            Usuario gravado = captor.getValue();
            assertThat(gravado.getRole()).isEqualTo(UsuarioRole.VENDEDOR);
            assertThat(gravado.getAtivo()).isTrue();
            assertThat(gravado.getSenha()).isEqualTo("$2a$10$hashDoJoao").isNotEqualTo("segredo1");

            assertThat(resposta.token()).isEqualTo("jwt-de-teste");
            assertThat(resposta.usuario().id()).isEqualTo(9L);
            assertThat(resposta.usuario().role()).isEqualTo(UsuarioRole.VENDEDOR);
        }

        @Test
        @DisplayName("recusa e-mail que ja esta cadastrado, sem gravar nada")
        void deveRecusarEmailDuplicado() {
            // Arrange
            when(usuarioRepository.existsByEmail("joao@crm.com")).thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> authService.registrar(cadastro))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("email");
            verify(usuarioRepository, never()).save(any());
            verify(jwtService, never()).gerar(any());
        }
    }
}
