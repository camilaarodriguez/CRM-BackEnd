package com.crmapi.sistemacrm.controller;

import com.crmapi.sistemacrm.dto.auth.LoginRequestDTO;
import com.crmapi.sistemacrm.dto.auth.LoginResponseDTO;
import com.crmapi.sistemacrm.dto.auth.RegistroRequestDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioResponseDTO;
import com.crmapi.sistemacrm.exception.BusinessException;
import com.crmapi.sistemacrm.exception.CredenciaisInvalidasException;
import com.crmapi.sistemacrm.exception.UsuarioInativoException;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import com.crmapi.sistemacrm.service.AuthService;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fatia web das rotas de autenticacao: sobe apenas o AuthController, o tratador
 * global de erros e a conversao JSON. O servico vem mockado, entao o que se verifica
 * aqui e o contrato HTTP - rota, codigo de status, validacao do corpo e formato da
 * resposta. A regra de autenticacao e coberta em AuthServiceImplTest, e a cadeia de
 * filtros do Spring Security em SegurancaConfigWebTest.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("/api/auth")
class AuthControllerTest {

    private static final String LOGIN_VALIDO = """
            {"email":"ana.moraes@crm.com","senha":"123456"}
            """;

    private static final String CADASTRO_VALIDO = """
            {"nome":"Ana Beatriz Moraes","email":"ana.moraes@crm.com","senha":"123456"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    private final UsuarioResponseDTO usuario = new UsuarioResponseDTO(3L, "Ana Beatriz Moraes",
            "ana.moraes@crm.com", UsuarioRole.VENDEDOR, true, LocalDateTime.now(), LocalDateTime.now());

    private final LoginResponseDTO resposta = new LoginResponseDTO("jwt-de-teste",
            Instant.parse("2026-11-26T20:00:00Z"), usuario);

    @Nested
    @DisplayName("POST /login com credenciais aceitas")
    class CaminhoFeliz {

        @Test
        @DisplayName("responde 200 com o token, o tipo, a expiracao e o usuario")
        void deveResponder200ComOToken() throws Exception {
            // Arrange
            when(authService.autenticar(any(LoginRequestDTO.class))).thenReturn(resposta);

            // Act + Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-de-teste"))
                    .andExpect(jsonPath("$.tipo").value("Bearer"))
                    .andExpect(jsonPath("$.expiraEm").value("2026-11-26T20:00:00Z"))
                    .andExpect(jsonPath("$.usuario.id").value(3))
                    .andExpect(jsonPath("$.usuario.role").value("VENDEDOR"));
        }

        @Test
        @DisplayName("nunca devolve a senha nem o hash no corpo da resposta")
        void naoDeveDevolverASenha() throws Exception {
            // Arrange
            when(authService.autenticar(any(LoginRequestDTO.class))).thenReturn(resposta);

            // Act + Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.usuario.senha").doesNotExist())
                    .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist());
        }

        @Test
        @DisplayName("repassa ao servico exatamente o e-mail e a senha recebidos")
        void deveRepassarAsCredenciaisAoServico() throws Exception {
            // Arrange
            when(authService.autenticar(any(LoginRequestDTO.class))).thenReturn(resposta);

            // Act
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_VALIDO))
                    .andExpect(status().isOk());

            // Assert
            verify(authService).autenticar(new LoginRequestDTO("ana.moraes@crm.com", "123456"));
        }
    }

    @Nested
    @DisplayName("POST /login com credenciais recusadas")
    class CredenciaisRecusadas {

        @Test
        @DisplayName("responde 401 quando o e-mail ou a senha nao conferem")
        void deveResponder401ParaCredencialInvalida() throws Exception {
            // Arrange
            when(authService.autenticar(any(LoginRequestDTO.class)))
                    .thenThrow(new CredenciaisInvalidasException());

            // Act + Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_VALIDO))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.mensagem").value("E-mail ou senha invalidos"))
                    .andExpect(jsonPath("$.caminho").value("/api/auth/login"));
        }

        @Test
        @DisplayName("responde 403 quando o usuario esta inativo")
        void deveResponder403ParaUsuarioInativo() throws Exception {
            // Arrange
            when(authService.autenticar(any(LoginRequestDTO.class)))
                    .thenThrow(new UsuarioInativoException());

            // Act + Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_VALIDO))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.mensagem").value("Este usuario esta inativo. Procure um administrador."));
        }
    }

    @Nested
    @DisplayName("POST /login com corpo invalido")
    class CorpoInvalido {

        @Test
        @DisplayName("responde 400 e lista os campos quando e-mail e senha vem em branco")
        void deveResponder400ListandoOsCamposObrigatorios() throws Exception {
            // Act + Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"\",\"senha\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value("Erro de validacao"))
                    .andExpect(jsonPath("$.detalhes").isArray())
                    .andExpect(jsonPath("$.detalhes", org.hamcrest.Matchers.hasItem("O email e obrigatorio")))
                    .andExpect(jsonPath("$.detalhes", org.hamcrest.Matchers.hasItem("A senha e obrigatoria")));
        }

        @Test
        @DisplayName("responde 400 quando o e-mail nao tem formato de e-mail")
        void deveResponder400ParaEmailMalFormado() throws Exception {
            // Act + Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"nao-e-um-email\",\"senha\":\"123456\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes",
                            org.hamcrest.Matchers.hasItem("O email informado e invalido")));
        }

        @Test
        @DisplayName("nao chega a consultar o servico quando o corpo nao passa na validacao")
        void naoDeveChamarOServicoComCorpoInvalido() throws Exception {
            // Act
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"\",\"senha\":\"\"}"))
                    .andExpect(status().isBadRequest());

            // Assert: a validacao barra antes, poupando uma consulta ao banco por requisicao invalida.
            verify(authService, never()).autenticar(any());
        }
    }

    @Nested
    @DisplayName("POST /registro")
    class Registro {

        @Test
        @DisplayName("responde 201 com o token e o Location do novo usuario")
        void deveResponder201ComOToken() throws Exception {
            // Arrange
            when(authService.registrar(any(RegistroRequestDTO.class))).thenReturn(resposta);

            // Act + Assert
            mockMvc.perform(post("/api/auth/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CADASTRO_VALIDO))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/usuarios/3"))
                    .andExpect(jsonPath("$.token").value("jwt-de-teste"))
                    .andExpect(jsonPath("$.usuario.role").value("VENDEDOR"));
        }

        @Test
        @DisplayName("ignora um perfil enviado no corpo: o DTO nem tem esse campo")
        void deveIgnorarPerfilEnviadoNoCorpo() throws Exception {
            // Arrange
            when(authService.registrar(any(RegistroRequestDTO.class))).thenReturn(resposta);

            // Act
            mockMvc.perform(post("/api/auth/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nome":"Ana","email":"ana@crm.com","senha":"123456","role":"ADMIN"}
                                    """))
                    .andExpect(status().isCreated());

            // Assert
            verify(authService).registrar(new RegistroRequestDTO("Ana", "ana@crm.com", "123456"));
        }

        @Test
        @DisplayName("responde 409 quando o e-mail ja esta cadastrado")
        void deveResponder409ParaEmailDuplicado() throws Exception {
            // Arrange
            when(authService.registrar(any(RegistroRequestDTO.class)))
                    .thenThrow(new BusinessException("Ja existe um usuario cadastrado com o email informado"));

            // Act + Assert
            mockMvc.perform(post("/api/auth/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CADASTRO_VALIDO))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensagem").value("Ja existe um usuario cadastrado com o email informado"));
        }

        @Test
        @DisplayName("responde 400 listando nome, e-mail e senha invalidos")
        void deveResponder400ParaCadastroInvalido() throws Exception {
            // Act + Assert
            mockMvc.perform(post("/api/auth/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"\",\"email\":\"x\",\"senha\":\"123\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes", org.hamcrest.Matchers.hasItem("O nome e obrigatorio")))
                    .andExpect(jsonPath("$.detalhes", org.hamcrest.Matchers.hasItem("O email informado e invalido")))
                    .andExpect(jsonPath("$.detalhes",
                            org.hamcrest.Matchers.hasItem("A senha deve ter entre 6 e 72 caracteres")));
            verify(authService, never()).registrar(any());
        }
    }
}
