package com.crmapi.sistemacrm.controller;

import com.crmapi.sistemacrm.dto.usuario.UsuarioCreateDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioResponseDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioStatusDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioUpdateDTO;
import com.crmapi.sistemacrm.exception.ErrorResponseDTO;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import com.crmapi.sistemacrm.service.UsuarioService;
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
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Equipe do CRM: perfis de acesso, disponibilidade e manutencao dos cadastros")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(
            summary = "Cadastra um usuario",
            description = "Cria um integrante da equipe. O e-mail e unico e o usuario ja nasce ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario cadastrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campos obrigatorios ausentes ou invalidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Ja existe um usuario com o e-mail informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar(@Valid @RequestBody UsuarioCreateDTO dto) {
        UsuarioResponseDTO criado = usuarioService.criar(dto);
        URI location = URI.create("/api/usuarios/" + criado.id());
        return ResponseEntity.created(location).body(criado);
    }

    @Operation(
            summary = "Lista usuarios com filtros e paginacao",
            description = "Retorna uma pagina de usuarios. Todos os filtros sao opcionais e podem ser combinados.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Parametro de filtro em formato invalido",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<Page<UsuarioResponseDTO>> listar(
            @Parameter(description = "Texto livre buscado no nome ou no e-mail", example = "ana")
            @RequestParam(required = false) String busca,
            @Parameter(description = "Perfil de acesso do usuario")
            @RequestParam(required = false) UsuarioRole role,
            @Parameter(description = "Filtra por disponibilidade do usuario", example = "true")
            @RequestParam(required = false) Boolean ativo,
            @ParameterObject Pageable pageable) {
        Page<UsuarioResponseDTO> pagina = usuarioService.listar(busca, role, ativo, pageable);
        return ResponseEntity.ok(pagina);
    }

    @Operation(summary = "Busca um usuario pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(
            @Parameter(description = "Identificador do usuario", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @Operation(
            summary = "Atualiza o cadastro de um usuario",
            description = "Altera nome, e-mail e perfil de acesso. A senha nao e alterada por este endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cadastro atualizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campos obrigatorios ausentes ou invalidos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "O e-mail informado ja pertence a outro usuario",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizar(
            @Parameter(description = "Identificador do usuario", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizar(id, dto));
    }

    @Operation(
            summary = "Ativa ou desativa um usuario",
            description = "Usuario inativo perde o acesso e deixa de receber novos clientes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Situacao atualizada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campo ativo ausente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<UsuarioResponseDTO> atualizarStatus(
            @Parameter(description = "Identificador do usuario", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UsuarioStatusDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizarStatus(id, dto));
    }

    @Operation(summary = "Exclui um usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario excluido"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @Parameter(description = "Identificador do usuario", example = "1")
            @PathVariable Long id) {
        usuarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
