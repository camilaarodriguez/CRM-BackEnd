package com.crmapi.sistemacrm.controller;

import com.crmapi.sistemacrm.dto.auth.LoginRequestDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioResponseDTO;
import com.crmapi.sistemacrm.exception.BusinessException;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import com.crmapi.sistemacrm.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fatia web do endpoint de login: sobe apenas o AuthController, o tratador global
 * de erros e a conversao JSON. O servico vem mockado, entao o que se verifica aqui
 * e o contrato HTTP - rota, codigo de status, validacao do corpo e formato da
 * resposta - e nao a regra de autenticacao, coberta em AuthServiceImplTest.
 */
@WebMvcTest(AuthController.class)
@DisplayName("POST /api/auth/login")
class AuthControllerTest {

    private static final String LOGIN_VALIDO = """
            {"email":"ana.moraes@crm.com","senha":"123456"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    private final UsuarioResponseDTO usuario = new UsuarioResponseDTO(3L, "Ana Beatriz Moraes",
            "ana.moraes@crm.com", UsuarioRole.VENDEDOR, true, LocalDateTime.now(), LocalDateTime.now());

    @Nested
    @DisplayName("Com credenciais aceitas")
    class CaminhoFeliz {

        @Test
        @DisplayName("responde 200 com os dados do usuario")
        void deveResponder200ComOUsuario() throws Exception {
            // Arrange
            when(authService.autenticar(any(LoginRequestDTO.class))).thenReturn(usuario);

            // Act + Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(3))
                    .andExpect(jsonPath("$.nome").value("Ana Beatriz Moraes"))
                    .andExpect(jsonPath("$.role").value("VENDEDOR"));
        }

        @Test
        @DisplayName("nunca devolve a senha nem o hash no corpo da resposta")
        void naoDeveDevolverASenha() throws Exception {
            // Arrange
            when(authService.autenticar(any(LoginRequestDTO.class))).thenReturn(usuario);

            // Act + Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.senha").doesNotExist())
                    .andExpect(jsonPath("$.senhaHash").doesNotExist());
        }

        @Test
        @DisplayName("repassa ao servico exatamente o e-mail e a senha recebidos")
        void deveRepassarAsCredenciaisAoServico() throws Exception {
            // Arrange
            when(authService.autenticar(any(LoginRequestDTO.class))).thenReturn(usuario);

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
    @DisplayName("Com credenciais recusadas")
    class CredenciaisRecusadas {

        @Test
        @DisplayName("responde 409 quando o servico recusa as credenciais")
        void deveResponder409ParaCredencialInvalida() throws Exception {
            // Arrange
            when(authService.autenticar(any(LoginRequestDTO.class)))
                    .thenThrow(new BusinessException("E-mail ou senha invalidos"));

            // Act + Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_VALIDO))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensagem").value("E-mail ou senha invalidos"))
                    .andExpect(jsonPath("$.caminho").value("/api/auth/login"));
        }

        @Test
        @DisplayName("responde 409 quando o usuario esta inativo")
        void deveResponder409ParaUsuarioInativo() throws Exception {
            // Arrange
            when(authService.autenticar(any(LoginRequestDTO.class)))
                    .thenThrow(new BusinessException("Este usuario esta inativo. Procure um administrador."));

            // Act + Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_VALIDO))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensagem").value("Este usuario esta inativo. Procure um administrador."));
        }
    }

    @Nested
    @DisplayName("Com corpo invalido")
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
}
