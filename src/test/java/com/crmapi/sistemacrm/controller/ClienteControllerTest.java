package com.crmapi.sistemacrm.controller;

import com.crmapi.sistemacrm.dto.cliente.ClienteCreateDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteReatribuirDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteResponseDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteStatusFunilDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteUpdateDTO;
import com.crmapi.sistemacrm.exception.BusinessException;
import com.crmapi.sistemacrm.exception.ResourceNotFoundException;
import com.crmapi.sistemacrm.model.enums.StatusFunil;
import com.crmapi.sistemacrm.service.ClienteService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fatia web de /api/clientes. Sobe o controller, o tratador global de erros, a
 * conversao JSON e a resolucao de Pageable; o servico vem mockado. As regras de
 * negocio ficam em ClienteServiceImplTest - aqui so se verifica o contrato HTTP.
 */
@WebMvcTest(ClienteController.class)
@DisplayName("/api/clientes")
class ClienteControllerTest {

    private static final String CLIENTE_JSON = """
            {"nome":"Mariana Souza","email":"mariana@tech.com","telefone":"(11) 98877-6543","vendedorId":3}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClienteService clienteService;

    private final ClienteResponseDTO resposta = new ClienteResponseDTO(10L, "Mariana Souza", "mariana@tech.com",
            "(11) 98877-6543", null, "Tech Ltda", null, 3L, "Carlos", StatusFunil.NOVO, true, null, null);

    @Nested
    @DisplayName("POST /api/clientes")
    class Criar {

        @Test
        @DisplayName("responde 201 com o Location do recurso criado")
        void deveCriarCliente() throws Exception {
            // Arrange
            when(clienteService.criar(any(ClienteCreateDTO.class))).thenReturn(resposta);

            // Act + Assert
            mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(CLIENTE_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/clientes/10"))
                    .andExpect(jsonPath("$.vendedorNome").value("Carlos"));
        }

        @Test
        @DisplayName("responde 409 quando o telefone ja pertence a outro cliente")
        void deveResponder409QuandoRegraDeNegocioViolada() throws Exception {
            // Arrange
            when(clienteService.criar(any(ClienteCreateDTO.class)))
                    .thenThrow(new BusinessException("Ja existe um cliente cadastrado com o telefone informado"));

            // Act + Assert
            mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(CLIENTE_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.erro").value("Violacao de regra de negocio"))
                    .andExpect(jsonPath("$.mensagem").value("Ja existe um cliente cadastrado com o telefone informado"));
        }

        @Test
        @DisplayName("responde 400 e nao chama o servico quando faltam campos obrigatorios")
        void deveResponder400QuandoFaltamCamposObrigatorios() throws Exception {
            // Act + Assert
            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"sem-nome-nem-telefone\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value("Erro de validacao"))
                    .andExpect(jsonPath("$.detalhes").isNotEmpty());

            verify(clienteService, never()).criar(any());
        }

        @Test
        @DisplayName("responde 400 quando o corpo nao e JSON valido")
        void deveResponder400QuandoOCorpoEstaQuebrado() throws Exception {
            // Act + Assert: um corpo ilegivel e erro de quem chamou, nao do servidor.
            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nome\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value("Corpo da requisicao invalido"));

            verify(clienteService, never()).criar(any());
        }
    }

    @Nested
    @DisplayName("Formato de telefone e documento")
    class Formato {

        private String corpoCom(String telefone, String documento) {
            return """
                    {"nome":"Mariana Souza","email":"mariana@tech.com",
                     "telefone":"%s","documento":"%s","vendedorId":3}
                    """.formatted(telefone, documento);
        }

        @Test
        @DisplayName("aceita telefone brasileiro com DDD e pontuacao")
        void deveAceitarTelefoneFormatado() throws Exception {
            // Arrange
            when(clienteService.criar(any(ClienteCreateDTO.class))).thenReturn(resposta);

            // Act + Assert
            mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                            .content(corpoCom("(11) 98877-6543", "412.559.870-11")))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("responde 400 quando o telefone tem letras")
        void deveRecusarTelefoneComLetras() throws Exception {
            // Act + Assert
            mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                            .content(corpoCom("abcdefghij", "412.559.870-11")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes", org.hamcrest.Matchers.hasItem(
                            org.hamcrest.Matchers.containsString("telefone"))));

            verify(clienteService, never()).criar(any());
        }

        @Test
        @DisplayName("responde 400 quando o telefone so tem simbolos")
        void deveRecusarTelefoneSoComSimbolos() throws Exception {
            // Act + Assert: dez caracteres, nenhum digito. Checar so o tamanho
            // deixaria isto passar.
            mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                            .content(corpoCom("(((((())))", "412.559.870-11")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("responde 400 quando o telefone tem digitos de menos")
        void deveRecusarTelefoneCurto() throws Exception {
            // Act + Assert
            mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                            .content(corpoCom("(11) 9887", "412.559.870-11")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("aceita CNPJ com 14 digitos")
        void deveAceitarCnpj() throws Exception {
            // Arrange
            when(clienteService.criar(any(ClienteCreateDTO.class))).thenReturn(resposta);

            // Act + Assert
            mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                            .content(corpoCom("(21) 99654-1200", "22.145.908/0001-73")))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("aceita cliente sem documento")
        void deveAceitarDocumentoVazio() throws Exception {
            // Arrange
            when(clienteService.criar(any(ClienteCreateDTO.class))).thenReturn(resposta);

            // Act + Assert: o documento e opcional e continua opcional.
            mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                            .content(corpoCom("(11) 98877-6543", "")))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("responde 400 quando o documento tem letras")
        void deveRecusarDocumentoComLetras() throws Exception {
            // Act + Assert
            mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                            .content(corpoCom("(11) 98877-6543", "nao-e-um-cpf")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes", org.hamcrest.Matchers.hasItem(
                            org.hamcrest.Matchers.containsString("documento"))));

            verify(clienteService, never()).criar(any());
        }

        @Test
        @DisplayName("responde 400 quando o documento tem quantidade de digitos invalida")
        void deveRecusarDocumentoComDigitosDeMenos() throws Exception {
            // Act + Assert: doze digitos nao e CPF nem CNPJ.
            mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON)
                            .content(corpoCom("(11) 98877-6543", "123456789012")))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/clientes")
    class Listar {

        @Test
        @DisplayName("repassa busca e filtro de funil ao servico e devolve a pagina")
        void deveListarClientesComFiltros() throws Exception {
            // Arrange
            when(clienteService.listar(eq("mari"), eq(StatusFunil.NOVO), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(resposta), PageRequest.of(0, 20), 1));

            // Act + Assert
            mockMvc.perform(get("/api/clientes").param("busca", "mari").param("status", "NOVO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("devolve uma pagina vazia quando nada casa com o filtro")
        void deveDevolverPaginaVaziaSemResultados() throws Exception {
            // Arrange
            when(clienteService.listar(eq("ninguem"), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

            // Act + Assert
            mockMvc.perform(get("/api/clientes").param("busca", "ninguem"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("responde 400 quando o filtro de funil nao e um status conhecido")
        void deveResponder400ParaStatusInexistente() throws Exception {
            // Act + Assert
            mockMvc.perform(get("/api/clientes").param("status", "ETAPA_QUE_NAO_EXISTE"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/clientes/{id}")
    class BuscarPorId {

        @Test
        @DisplayName("responde 200 com o cliente")
        void deveBuscarClientePorId() throws Exception {
            // Arrange
            when(clienteService.buscarPorId(10L)).thenReturn(resposta);

            // Act + Assert
            mockMvc.perform(get("/api/clientes/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome").value("Mariana Souza"));
        }

        @Test
        @DisplayName("responde 404 quando o cliente nao existe")
        void deveResponder404QuandoNaoExiste() throws Exception {
            // Arrange
            when(clienteService.buscarPorId(99L)).thenThrow(new ResourceNotFoundException("Cliente nao encontrado"));

            // Act + Assert
            mockMvc.perform(get("/api/clientes/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.mensagem").value("Cliente nao encontrado"))
                    .andExpect(jsonPath("$.caminho").value("/api/clientes/99"));
        }

        @Test
        @DisplayName("responde 400 quando o id nao e um numero")
        void deveResponder400QuandoOIdNaoENumerico() throws Exception {
            // Act + Assert
            mockMvc.perform(get("/api/clientes/abc"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Atualizacoes")
    class Atualizar {

        @Test
        @DisplayName("PUT responde 200 com o cliente atualizado")
        void deveAtualizarCliente() throws Exception {
            // Arrange
            when(clienteService.atualizar(eq(10L), any(ClienteUpdateDTO.class))).thenReturn(resposta);

            // Act + Assert
            mockMvc.perform(put("/api/clientes/10").contentType(MediaType.APPLICATION_JSON).content(CLIENTE_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10));
        }

        @Test
        @DisplayName("PATCH /status-funil move o cliente de etapa")
        void deveAtualizarStatusDoFunil() throws Exception {
            // Arrange
            when(clienteService.atualizarStatusFunil(eq(10L), any(ClienteStatusFunilDTO.class))).thenReturn(resposta);

            // Act + Assert
            mockMvc.perform(patch("/api/clientes/10/status-funil")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"statusFunil\":\"NEGOCIACAO\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10));
        }

        @Test
        @DisplayName("PATCH /status-funil responde 400 sem o campo obrigatorio")
        void deveResponder400SemOStatusFunil() throws Exception {
            // Act + Assert
            mockMvc.perform(patch("/api/clientes/10/status-funil")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalhes").isNotEmpty());
        }

        @Test
        @DisplayName("PATCH /reatribuir troca o vendedor responsavel")
        void deveReatribuirCliente() throws Exception {
            // Arrange
            when(clienteService.reatribuir(eq(10L), any(ClienteReatribuirDTO.class))).thenReturn(resposta);

            // Act + Assert
            mockMvc.perform(patch("/api/clientes/10/reatribuir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"novoVendedorId\":4,\"motivo\":\"Ferias\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10));
        }

        @Test
        @DisplayName("PATCH /reatribuir responde 404 quando o novo vendedor nao existe")
        void deveResponder404QuandoONovoVendedorNaoExiste() throws Exception {
            // Arrange
            when(clienteService.reatribuir(eq(10L), any(ClienteReatribuirDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Usuario nao encontrado"));

            // Act + Assert
            mockMvc.perform(patch("/api/clientes/10/reatribuir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"novoVendedorId\":404,\"motivo\":\"Ferias\"}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/clientes/{id}")
    class Deletar {

        @Test
        @DisplayName("responde 204 e delega a exclusao ao servico")
        void deveDeletarCliente() throws Exception {
            // Act
            mockMvc.perform(delete("/api/clientes/10"))
                    .andExpect(status().isNoContent());

            // Assert
            verify(clienteService).deletar(10L);
        }

        @Test
        @DisplayName("responde 404 quando o cliente nao existe")
        void deveResponder404AoExcluirInexistente() throws Exception {
            // Arrange
            doThrow(new ResourceNotFoundException("Cliente nao encontrado")).when(clienteService).deletar(99L);

            // Act + Assert
            mockMvc.perform(delete("/api/clientes/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.mensagem").value("Cliente nao encontrado"));
        }
    }
}
