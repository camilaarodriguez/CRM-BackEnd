package com.crmapi.sistemacrm.controller;

import com.crmapi.sistemacrm.dto.usuario.UsuarioCreateDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioResponseDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioSenhaDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioStatusDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioUpdateDTO;
import com.crmapi.sistemacrm.exception.BusinessException;
import com.crmapi.sistemacrm.exception.ResourceNotFoundException;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import com.crmapi.sistemacrm.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fatia web de /api/usuarios. O servico vem mockado; o que se verifica aqui e o
 * contrato HTTP, incluindo o cuidado de a senha nunca voltar no corpo.
 */
@WebMvcTest(UsuarioController.class)
@DisplayName("/api/usuarios")
class UsuarioControllerTest {

    private static final String USUARIO_JSON = """
            {"nome":"Ana Moraes","email":"ana@crm.com","senha":"123456","role":"VENDEDOR"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    private final UsuarioResponseDTO resposta =
            new UsuarioResponseDTO(1L, "Ana Moraes", "ana@crm.com", UsuarioRole.VENDEDOR, true, null, null);

    @Nested
    @DisplayName("POST /api/usuarios")
    class Criar {

        @Test
        @DisplayName("responde 201 com o Location do recurso criado")
        void deveCriarUsuario() throws Exception {
            // Arrange
            when(usuarioService.criar(any(UsuarioCreateDTO.class))).thenReturn(resposta);

            // Act + Assert
            mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(USUARIO_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/usuarios/1"))
                    .andExpect(jsonPath("$.email").value("ana@crm.com"));
        }

        @Test
        @DisplayName("nunca devolve a senha enviada no cadastro")
        void naoDeveEcoarASenhaNoCorpoDaResposta() throws Exception {
            // Arrange
            when(usuarioService.criar(any(UsuarioCreateDTO.class))).thenReturn(resposta);

            // Act + Assert
            mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(USUARIO_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.senha").doesNotExist())
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("123456"))));
        }

        @Test
        @DisplayName("responde 400 e nao chama o servico quando os dados sao invalidos")
        void deveResponder400QuandoDadosInvalidos() throws Exception {
            // Act + Assert
            mockMvc.perform(post("/api/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"invalido\",\"senha\":\"1\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value("Erro de validacao"))
                    .andExpect(jsonPath("$.detalhes").isNotEmpty());

            verify(usuarioService, never()).criar(any());
        }

        @Test
        @DisplayName("responde 409 quando o e-mail ja esta cadastrado")
        void deveResponder409ParaEmailDuplicado() throws Exception {
            // Arrange
            when(usuarioService.criar(any(UsuarioCreateDTO.class)))
                    .thenThrow(new BusinessException("Ja existe um usuario cadastrado com este email"));

            // Act + Assert
            mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(USUARIO_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensagem").value("Ja existe um usuario cadastrado com este email"));
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultar {

        @Test
        @DisplayName("GET lista filtrando pela busca informada")
        void deveListarUsuarios() throws Exception {
            // Arrange
            when(usuarioService.listar(eq("ana"), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(resposta), PageRequest.of(0, 20), 1));

            // Act + Assert
            mockMvc.perform(get("/api/usuarios").param("busca", "ana"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].senha").doesNotExist());
        }

        @Test
        @DisplayName("GET /{id} responde 200 com o usuario")
        void deveBuscarUsuarioPorId() throws Exception {
            // Arrange
            when(usuarioService.buscarPorId(1L)).thenReturn(resposta);

            // Act + Assert
            mockMvc.perform(get("/api/usuarios/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome").value("Ana Moraes"));
        }

        @Test
        @DisplayName("GET /{id} responde 404 quando o usuario nao existe")
        void deveResponder404QuandoUsuarioNaoExiste() throws Exception {
            // Arrange
            when(usuarioService.buscarPorId(99L)).thenThrow(new ResourceNotFoundException("Usuario nao encontrado"));

            // Act + Assert
            mockMvc.perform(get("/api/usuarios/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.erro").value("Recurso nao encontrado"))
                    .andExpect(jsonPath("$.caminho").value("/api/usuarios/99"));
        }
    }

    @Nested
    @DisplayName("Atualizacoes")
    class Atualizar {

        @Test
        @DisplayName("PUT responde 200 com o usuario atualizado")
        void deveAtualizarUsuario() throws Exception {
            // Arrange
            when(usuarioService.atualizar(eq(1L), any(UsuarioUpdateDTO.class))).thenReturn(resposta);

            // Act + Assert
            mockMvc.perform(put("/api/usuarios/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"Ana Moraes\",\"email\":\"ana@crm.com\",\"role\":\"VENDEDOR\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("PATCH /status ativa e desativa o usuario")
        void deveAtualizarStatusDoUsuario() throws Exception {
            // Arrange
            when(usuarioService.atualizarStatus(eq(1L), any(UsuarioStatusDTO.class))).thenReturn(resposta);

            // Act + Assert
            mockMvc.perform(patch("/api/usuarios/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ativo\":true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ativo").value(true));
        }

        @Test
        @DisplayName("PUT responde 400 quando o e-mail nao tem formato valido")
        void deveResponder400ParaEmailMalFormadoNaAtualizacao() throws Exception {
            // Act + Assert
            mockMvc.perform(put("/api/usuarios/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\":\"Ana\",\"email\":\"isso-nao-e-email\",\"role\":\"VENDEDOR\"}"))
                    .andExpect(status().isBadRequest());

            verify(usuarioService, never()).atualizar(any(), any());
        }
    }

    @Nested
    @DisplayName("PATCH /api/usuarios/{id}/senha")
    class AlterarSenha {

        @Test
        @DisplayName("responde 204 e repassa as duas senhas ao servico")
        void deveAlterarASenha() throws Exception {
            // Act
            mockMvc.perform(patch("/api/usuarios/1/senha")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"senhaAtual\":\"123456\",\"novaSenha\":\"nova-senha-forte\"}"))
                    .andExpect(status().isNoContent());

            // Assert
            verify(usuarioService).alterarSenha(1L, new UsuarioSenhaDTO("123456", "nova-senha-forte"));
        }

        @Test
        @DisplayName("responde 409 quando a senha atual nao confere")
        void deveResponder409QuandoASenhaAtualEstaErrada() throws Exception {
            // Arrange
            doThrow(new BusinessException("A senha atual nao confere"))
                    .when(usuarioService).alterarSenha(eq(1L), any(UsuarioSenhaDTO.class));

            // Act + Assert
            mockMvc.perform(patch("/api/usuarios/1/senha")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"senhaAtual\":\"errada\",\"novaSenha\":\"nova-senha-forte\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensagem").value("A senha atual nao confere"));
        }

        @Test
        @DisplayName("responde 400 quando a nova senha e curta demais")
        void deveResponder400ParaNovaSenhaCurta() throws Exception {
            // Act + Assert
            mockMvc.perform(patch("/api/usuarios/1/senha")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"senhaAtual\":\"123456\",\"novaSenha\":\"123\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes").isNotEmpty());

            verify(usuarioService, never()).alterarSenha(any(), any());
        }

        @Test
        @DisplayName("responde 404 quando o usuario nao existe")
        void deveResponder404QuandoOUsuarioNaoExiste() throws Exception {
            // Arrange
            doThrow(new ResourceNotFoundException("Usuario nao encontrado"))
                    .when(usuarioService).alterarSenha(eq(99L), any(UsuarioSenhaDTO.class));

            // Act + Assert
            mockMvc.perform(patch("/api/usuarios/99/senha")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"senhaAtual\":\"123456\",\"novaSenha\":\"nova-senha-forte\"}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/usuarios/{id}")
    class Deletar {

        @Test
        @DisplayName("responde 204 e delega a exclusao ao servico")
        void deveDeletarUsuario() throws Exception {
            // Act
            mockMvc.perform(delete("/api/usuarios/1"))
                    .andExpect(status().isNoContent());

            // Assert
            verify(usuarioService).deletar(1L);
        }

        @Test
        @DisplayName("responde 409 quando o usuario ainda tem clientes na carteira")
        void deveResponder409QuandoOUsuarioTemVinculos() throws Exception {
            // Arrange
            doThrow(new BusinessException("Nao foi possivel excluir o usuario"))
                    .when(usuarioService).deletar(1L);

            // Act + Assert
            mockMvc.perform(delete("/api/usuarios/1"))
                    .andExpect(status().isConflict());
        }
    }
}
