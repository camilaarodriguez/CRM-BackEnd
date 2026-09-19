package com.crmapi.sistemacrm.controller;

import com.crmapi.sistemacrm.dto.cliente.ClienteCreateDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteReatribuirDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteResponseDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteStatusFunilDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteUpdateDTO;
import com.crmapi.sistemacrm.exception.ErrorResponseDTO;
import com.crmapi.sistemacrm.model.enums.StatusFunil;
import com.crmapi.sistemacrm.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Carteira de clientes, funil de vendas e distribuicao de leads entre vendedores")
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(
            summary = "Cadastra um cliente",
            description = """
                    Cria o cliente e o vincula ao vendedor informado. Junto com o cadastro o sistema
                    abre automaticamente a conversa do cliente, registra a atribuicao inicial no
                    historico e dispara uma notificacao externa para a equipe.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente cadastrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClienteResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campos obrigatorios ausentes ou invalidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vendedor informado nao existe",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> criar(@Valid @RequestBody ClienteCreateDTO dto) {
        ClienteResponseDTO criado = clienteService.criar(dto);
        URI location = URI.create("/api/clientes/" + criado.id());
        return ResponseEntity.created(location).body(criado);
    }

    @Operation(
            summary = "Lista clientes com filtros e paginacao",
            description = """
                    Retorna uma pagina de clientes. Todos os filtros sao opcionais e podem ser combinados.
                    Por padrao apenas clientes ativos sao devolvidos.""")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Parametro de filtro em formato invalido",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<Page<ClienteResponseDTO>> listar(
            @Parameter(description = "Texto livre buscado no nome ou no e-mail do cliente", example = "mariana")
            @RequestParam(required = false) String busca,
            @Parameter(description = "Etapa do funil de vendas")
            @RequestParam(required = false) StatusFunil status,
            @Parameter(description = "Identificador do vendedor responsavel", example = "3")
            @RequestParam(required = false) Long vendedorId,
            @Parameter(description = "Quando true, inclui tambem os clientes inativos", example = "false")
            @RequestParam(required = false) Boolean incluirInativos,
            @ParameterObject Pageable pageable) {
        Page<ClienteResponseDTO> pagina = clienteService.listar(busca, status, vendedorId, incluirInativos, pageable);
        return ResponseEntity.ok(pagina);
    }

    @Operation(summary = "Busca um cliente pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClienteResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cliente nao encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(
            @Parameter(description = "Identificador do cliente", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @Operation(
            summary = "Atualiza o cadastro de um cliente",
            description = "Substitui os dados cadastrais. A etapa do funil nao muda por aqui: use PATCH /status-funil.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cadastro atualizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClienteResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campos obrigatorios ausentes ou invalidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cliente ou vendedor nao encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> atualizar(
            @Parameter(description = "Identificador do cliente", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ClienteUpdateDTO dto) {
        return ResponseEntity.ok(clienteService.atualizar(id, dto));
    }

    @Operation(
            summary = "Move o cliente de etapa no funil",
            description = "Altera apenas o status do funil, sem tocar no restante do cadastro.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Etapa atualizada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClienteResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Status do funil ausente ou invalido",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cliente nao encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PatchMapping("/{id}/status-funil")
    public ResponseEntity<ClienteResponseDTO> atualizarStatusFunil(
            @Parameter(description = "Identificador do cliente", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ClienteStatusFunilDTO dto) {
        return ResponseEntity.ok(clienteService.atualizarStatusFunil(id, dto));
    }

    @Operation(
            summary = "Reatribui o cliente a outro vendedor",
            description = """
                    Transfere o cliente e a conversa dele para o novo vendedor, grava o motivo no
                    historico de atribuicoes e notifica a equipe pelo canal externo.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente reatribuido",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClienteResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Novo vendedor nao informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cliente ou vendedor nao encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PatchMapping("/{id}/reatribuir")
    public ResponseEntity<ClienteResponseDTO> reatribuir(
            @Parameter(description = "Identificador do cliente", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ClienteReatribuirDTO dto) {
        return ResponseEntity.ok(clienteService.reatribuir(id, dto));
    }

    @Operation(
            summary = "Exclui um cliente",
            description = "Remove o cliente definitivamente, junto com a conversa e o historico ligados a ele.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cliente excluido"),
            @ApiResponse(responseCode = "404", description = "Cliente nao encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @Parameter(description = "Identificador do cliente", example = "1")
            @PathVariable Long id) {
        clienteService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
