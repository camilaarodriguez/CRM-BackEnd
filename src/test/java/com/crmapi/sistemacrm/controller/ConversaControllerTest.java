package com.crmapi.sistemacrm.controller;

import com.crmapi.sistemacrm.dto.conversa.ConversaResponseDTO;
import com.crmapi.sistemacrm.exception.ResourceNotFoundException;
import com.crmapi.sistemacrm.model.enums.StatusConversa;
import com.crmapi.sistemacrm.service.ConversaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fatia web de /api/conversas: a fila de atendimento. O vendedor enxerga apenas a
 * propria fila, e a gestao enxerga a operacao inteira - e esse recorte que o
 * parametro vendedorId expressa no contrato HTTP.
 */
@WebMvcTest(ConversaController.class)
@DisplayName("/api/conversas")
class ConversaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConversaService conversaService;

    private final ConversaResponseDTO resposta = new ConversaResponseDTO(20L, 10L, "Mariana", 3L, "Carlos",
            StatusConversa.ABERTA, 2, null, null, null, null);

    @Nested
    @DisplayName("GET /api/conversas")
    class Listar {

        @Test
        @DisplayName("filtra a fila pelo vendedor informado")
        void deveListarConversasDoVendedor() throws Exception {
            // Arrange
            when(conversaService.listar(eq(3L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(resposta), PageRequest.of(0, 20), 1));

            // Act + Assert
            mockMvc.perform(get("/api/conversas").param("vendedorId", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].clienteNome").value("Mariana"))
                    .andExpect(jsonPath("$.content[0].vendedorNome").value("Carlos"));
        }

        @Test
        @DisplayName("sem vendedorId devolve a operacao inteira")
        void deveListarTodasAsConversasSemFiltro() throws Exception {
            // Arrange
            when(conversaService.listar(isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(resposta), PageRequest.of(0, 20), 1));

            // Act
            mockMvc.perform(get("/api/conversas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));

            // Assert: o filtro chega nulo ao servico, e nao como zero ou string vazia.
            verify(conversaService).listar(isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("respeita o tamanho de pagina pedido")
        void deveRespeitarAPaginacaoPedida() throws Exception {
            // Arrange
            when(conversaService.listar(isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(resposta), PageRequest.of(1, 5), 11));

            // Act + Assert
            mockMvc.perform(get("/api/conversas").param("page", "1").param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size").value(5))
                    .andExpect(jsonPath("$.number").value(1))
                    .andExpect(jsonPath("$.totalElements").value(11));
        }

        @Test
        @DisplayName("devolve lista vazia quando o vendedor nao tem conversas")
        void deveDevolverFilaVazia() throws Exception {
            // Arrange
            when(conversaService.listar(eq(7L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

            // Act + Assert
            mockMvc.perform(get("/api/conversas").param("vendedorId", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("responde 400 quando vendedorId nao e um numero")
        void deveResponder400ParaVendedorIdInvalido() throws Exception {
            // Act + Assert
            mockMvc.perform(get("/api/conversas").param("vendedorId", "tres"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value("Parametro invalido"));
        }
    }

    @Nested
    @DisplayName("GET /api/conversas/{id}")
    class BuscarPorId {

        @Test
        @DisplayName("responde 200 com a conversa e o contador de nao lidas")
        void deveBuscarConversaPorId() throws Exception {
            // Arrange
            when(conversaService.buscarPorId(20L)).thenReturn(resposta);

            // Act + Assert
            mockMvc.perform(get("/api/conversas/20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(20))
                    .andExpect(jsonPath("$.naoLidas").value(2))
                    .andExpect(jsonPath("$.status").value("ABERTA"));
        }

        @Test
        @DisplayName("responde 404 quando a conversa nao existe")
        void deveResponder404QuandoAConversaNaoExiste() throws Exception {
            // Arrange
            when(conversaService.buscarPorId(99L)).thenThrow(new ResourceNotFoundException("Conversa nao encontrada"));

            // Act + Assert
            mockMvc.perform(get("/api/conversas/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.mensagem").value("Conversa nao encontrada"))
                    .andExpect(jsonPath("$.caminho").value("/api/conversas/99"));
        }
    }
}
