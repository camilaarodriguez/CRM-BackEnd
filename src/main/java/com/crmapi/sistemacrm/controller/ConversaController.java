package com.crmapi.sistemacrm.controller;

import com.crmapi.sistemacrm.dto.conversa.ConversaResponseDTO;
import com.crmapi.sistemacrm.exception.ErrorResponseDTO;
import com.crmapi.sistemacrm.service.ConversaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversas")
@RequiredArgsConstructor
@Tag(name = "Conversas", description = "Fila de atendimento dos clientes. A conversa e aberta junto com o cadastro do cliente")
public class ConversaController {

    private final ConversaService conversaService;

    @Operation(
            summary = "Lista conversas com paginacao",
            description = """
                    Retorna uma pagina de conversas. Sem o filtro de vendedor devolve a operacao inteira;
                    com o filtro, devolve apenas a fila daquele vendedor.""")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Parametro de filtro em formato invalido",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<Page<ConversaResponseDTO>> listar(
            @Parameter(description = "Identificador do vendedor responsavel pela conversa", example = "3")
            @RequestParam(required = false) Long vendedorId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(conversaService.listar(vendedorId, pageable));
    }

    @Operation(summary = "Busca uma conversa pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversa encontrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConversaResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Conversa nao encontrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ConversaResponseDTO> buscarPorId(
            @Parameter(description = "Identificador da conversa", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(conversaService.buscarPorId(id));
    }
}
