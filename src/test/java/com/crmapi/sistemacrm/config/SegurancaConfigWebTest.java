package com.crmapi.sistemacrm.config;

import com.crmapi.sistemacrm.controller.AuthController;
import com.crmapi.sistemacrm.controller.ClienteController;
import com.crmapi.sistemacrm.controller.UsuarioController;
import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import com.crmapi.sistemacrm.repository.UsuarioRepository;
import com.crmapi.sistemacrm.security.JwtService;
import com.crmapi.sistemacrm.security.RespostaErroSeguranca;
import com.crmapi.sistemacrm.service.AuthService;
import com.crmapi.sistemacrm.service.ClienteService;
import com.crmapi.sistemacrm.service.UsuarioService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cadeia de filtros do Spring Security de ponta a ponta, com a SegurancaConfig real:
 * sem token a API responde 401, com perfil insuficiente responde 403, e o CORS so
 * libera as origens configuradas. Os servicos vem mockados - o que se verifica aqui
 * e quem passa pela porta, nao a regra de negocio.
 */
@WebMvcTest({ClienteController.class, UsuarioController.class, AuthController.class})
@Import({SegurancaConfig.class, RespostaErroSeguranca.class})
@DisplayName("SegurancaConfig: autenticacao, perfis e CORS na API")
class SegurancaConfigWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private ClienteService clienteService;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private AuthService authService;

    @BeforeEach
    void tokensConhecidos() {
        when(jwtService.extrairEmail(anyString())).thenReturn(Optional.empty());
        cadastrarToken("token-admin", UsuarioRole.ADMIN);
        cadastrarToken("token-gerente", UsuarioRole.GERENTE);
        cadastrarToken("token-vendedor", UsuarioRole.VENDEDOR);
    }

    private void cadastrarToken(String token, UsuarioRole role) {
        String email = role.name().toLowerCase() + "@crm.com";
        when(jwtService.extrairEmail(token)).thenReturn(Optional.of(email));
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(
                Usuario.builder().id(1L).email(email).role(role).ativo(true).build()));
    }

    private static MockHttpServletRequestBuilder comToken(MockHttpServletRequestBuilder requisicao, String token) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    @Nested
    @DisplayName("Sem autenticacao valida")
    class SemAutenticacao {

        @Test
        @DisplayName("responde 401 em JSON quando nao ha token")
        void deveResponder401SemToken() throws Exception {
            mockMvc.perform(get("/api/clientes"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.erro").value("Nao autenticado"))
                    .andExpect(jsonPath("$.caminho").value("/api/clientes"));
        }

        @Test
        @DisplayName("responde 401 quando o token e invalido ou expirado")
        void deveResponder401ComTokenInvalido() throws Exception {
            mockMvc.perform(comToken(get("/api/clientes"), "token-falsificado"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("libera o login sem token")
        void deveLiberarOLoginSemToken() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"ana@crm.com\",\"senha\":\"123456\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Com token valido")
    class ComToken {

        @Test
        @DisplayName("qualquer perfil autenticado lista a carteira de clientes")
        void deveLiberarRotaAutenticada() throws Exception {
            mockMvc.perform(comToken(get("/api/clientes"), "token-vendedor"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("qualquer perfil autenticado consulta a equipe e troca a propria senha")
        void deveLiberarConsultaDaEquipeEDaSenha() throws Exception {
            mockMvc.perform(comToken(get("/api/usuarios"), "token-vendedor"))
                    .andExpect(status().isOk());
            mockMvc.perform(comToken(patch("/api/usuarios/1/senha"), "token-vendedor")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"senhaAtual\":\"123456\",\"novaSenha\":\"654321\"}"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("responde 403 em JSON quando o vendedor tenta reatribuir um cliente")
        void deveResponder403ParaVendedorReatribuindo() throws Exception {
            mockMvc.perform(comToken(patch("/api/clientes/10/reatribuir"), "token-vendedor")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"novoVendedorId\":4,\"motivo\":\"Ferias\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.erro").value("Acesso negado"));
        }

        @Test
        @DisplayName("gerente reatribui clientes")
        void deveLiberarReatribuicaoParaGerente() throws Exception {
            mockMvc.perform(comToken(patch("/api/clientes/10/reatribuir"), "token-gerente")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"novoVendedorId\":4,\"motivo\":\"Ferias\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("so a gestao altera a equipe: vendedor recebe 403 e admin consegue excluir")
        void deveRestringirAlteracaoDaEquipeAGestao() throws Exception {
            mockMvc.perform(comToken(delete("/api/usuarios/5"), "token-vendedor"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(comToken(delete("/api/usuarios/5"), "token-admin"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("CORS")
    class Cors {

        @Test
        @DisplayName("autoriza o preflight vindo de uma origem configurada")
        void deveAutorizarOrigemConfigurada() throws Exception {
            mockMvc.perform(options("/api/clientes")
                            .header(HttpHeaders.ORIGIN, "http://crm.teste")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://crm.teste"));
        }

        @Test
        @DisplayName("recusa o preflight vindo de uma origem desconhecida")
        void deveRecusarOrigemDesconhecida() throws Exception {
            mockMvc.perform(options("/api/clientes")
                            .header(HttpHeaders.ORIGIN, "http://site-malicioso.com")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                    .andExpect(status().isForbidden());
        }
    }
}
