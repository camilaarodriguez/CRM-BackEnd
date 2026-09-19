package com.crmapi.sistemacrm.controller;

import com.crmapi.sistemacrm.dto.mensagem.MensagemCreateDTO;
import com.crmapi.sistemacrm.dto.mensagem.MensagemResponseDTO;
import com.crmapi.sistemacrm.exception.ErrorResponseDTO;
import com.crmapi.sistemacrm.service.MensagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mensagens")
@RequiredArgsConstructor
@Tag(name = "Mensagens", description = "Historico e envio de mensagens dentro de uma conversa")
public class MensagemController {

    private final MensagemService mensagemService;

    @Operation(
            summary = "Envia uma mensagem na conversa",
            description = """
                    Registra a mensagem com direcao de saida e status de entrega inicial, associa o
                    vendedor responsavel pela conversa como autor e atualiza a data da ultima mensagem.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mensagem registrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MensagemResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campos obrigatorios ausentes ou invalidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Conversa nao encontrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<MensagemResponseDTO> enviar(@Valid @RequestBody MensagemCreateDTO dto) {
        return ResponseEntity.status(201).body(mensagemService.enviar(dto));
    }

    @Operation(
            summary = "Lista as mensagens de uma conversa",
            description = "Devolve o historico completo em ordem cronologica, da mais antiga para a mais recente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historico da conversa",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = MensagemResponseDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Conversa nao encontrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/conversa/{conversaId}")
    public ResponseEntity<List<MensagemResponseDTO>> listarPorConversa(
            @Parameter(description = "Identificador da conversa", example = "1")
            @PathVariable Long conversaId) {
        return ResponseEntity.ok(mensagemService.listarPorConversa(conversaId));
    }
}
