package com.crmapi.sistemacrm.controller;

import com.crmapi.sistemacrm.dto.mensagem.MensagemCreateDTO;
import com.crmapi.sistemacrm.dto.mensagem.MensagemResponseDTO;
import com.crmapi.sistemacrm.exception.ResourceNotFoundException;
import com.crmapi.sistemacrm.model.enums.DirecaoMensagem;
import com.crmapi.sistemacrm.model.enums.StatusEntrega;
import com.crmapi.sistemacrm.model.enums.TipoMensagem;
import com.crmapi.sistemacrm.service.MensagemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fatia web de /api/mensagens: o historico e o envio dentro de um atendimento.
 */
@WebMvcTest(MensagemController.class)
@DisplayName("/api/mensagens")
class MensagemControllerTest {

    private static final String MENSAGEM_JSON = """
            {"conversaId":20,"tipo":"TEXTO","conteudo":"Ola, Mariana!"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MensagemService mensagemService;

    private final MensagemResponseDTO resposta = new MensagemResponseDTO(1L, 20L, DirecaoMensagem.SAIDA,
            TipoMensagem.TEXTO, "Ola, Mariana!", null, StatusEntrega.ENVIADA, 3L, "Carlos", null);

    @Nested
    @DisplayName("POST /api/mensagens")
    class Enviar {

        @Test
        @DisplayName("responde 201 com a mensagem registrada")
        void deveEnviarMensagem() throws Exception {
            // Arrange
            when(mensagemService.enviar(any(MensagemCreateDTO.class))).thenReturn(resposta);

            // Act + Assert
            mockMvc.perform(post("/api/mensagens").contentType(MediaType.APPLICATION_JSON).content(MENSAGEM_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.direcao").value("SAIDA"))
                    .andExpect(jsonPath("$.statusEntrega").value("ENVIADA"))
                    .andExpect(jsonPath("$.enviadaPorNome").value("Carlos"));
        }

        @Test
        @DisplayName("repassa ao servico exatamente o que veio no corpo")
        void deveRepassarOConteudoAoServico() throws Exception {
            // Arrange
            when(mensagemService.enviar(any(MensagemCreateDTO.class))).thenReturn(resposta);

            // Act
            mockMvc.perform(post("/api/mensagens").contentType(MediaType.APPLICATION_JSON).content(MENSAGEM_JSON))
                    .andExpect(status().isCreated());

            // Assert
            verify(mensagemService).enviar(new MensagemCreateDTO(20L, TipoMensagem.TEXTO, "Ola, Mariana!"));
        }

        @Test
        @DisplayName("responde 400 quando o conteudo vem vazio")
        void deveResponder400ParaConteudoVazio() throws Exception {
            // Act + Assert
            mockMvc.perform(post("/api/mensagens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"conversaId\":20,\"tipo\":\"TEXTO\",\"conteudo\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes",
                            org.hamcrest.Matchers.hasItem("O conteudo e obrigatorio")));

            verify(mensagemService, never()).enviar(any());
        }

        @Test
        @DisplayName("responde 400 quando falta a conversa de destino")
        void deveResponder400SemConversaId() throws Exception {
            // Act + Assert
            mockMvc.perform(post("/api/mensagens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tipo\":\"TEXTO\",\"conteudo\":\"Ola\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes",
                            org.hamcrest.Matchers.hasItem("O conversaId e obrigatorio")));
        }

        @Test
        @DisplayName("responde 400 quando o tipo de mensagem nao existe")
        void deveResponder400ParaTipoDesconhecido() throws Exception {
            // Act + Assert: so TEXTO e aceito; qualquer outro valor nem chega ao servico.
            mockMvc.perform(post("/api/mensagens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"conversaId\":20,\"tipo\":\"VIDEO\",\"conteudo\":\"Ola\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value("Corpo da requisicao invalido"));

            verify(mensagemService, never()).enviar(any());
        }

        @Test
        @DisplayName("responde 404 quando a conversa de destino nao existe")
        void deveResponder404ParaConversaInexistente() throws Exception {
            // Arrange
            when(mensagemService.enviar(any(MensagemCreateDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Conversa nao encontrada"));

            // Act + Assert
            mockMvc.perform(post("/api/mensagens").contentType(MediaType.APPLICATION_JSON).content(MENSAGEM_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.mensagem").value("Conversa nao encontrada"));
        }
    }

    @Nested
    @DisplayName("GET /api/mensagens/conversa/{conversaId}")
    class ListarPorConversa {

        @Test
        @DisplayName("responde 200 com o historico da conversa")
        void deveListarMensagensDaConversa() throws Exception {
            // Arrange
            when(mensagemService.listarPorConversa(20L)).thenReturn(List.of(resposta));

            // Act + Assert
            mockMvc.perform(get("/api/mensagens/conversa/20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].conteudo").value("Ola, Mariana!"));
        }

        @Test
        @DisplayName("responde 200 com lista vazia quando a conversa ainda nao tem mensagens")
        void deveDevolverHistoricoVazio() throws Exception {
            // Arrange
            when(mensagemService.listarPorConversa(21L)).thenReturn(List.of());

            // Act + Assert: conversa sem mensagens e lista vazia, nao 404.
            mockMvc.perform(get("/api/mensagens/conversa/21"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("responde 404 quando a conversa nao existe")
        void deveResponder404ParaConversaInexistente() throws Exception {
            // Arrange
            when(mensagemService.listarPorConversa(99L))
                    .thenThrow(new ResourceNotFoundException("Conversa nao encontrada"));

            // Act + Assert
            mockMvc.perform(get("/api/mensagens/conversa/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.caminho").value("/api/mensagens/conversa/99"));
        }
    }
}
