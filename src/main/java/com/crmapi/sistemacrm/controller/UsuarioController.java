package com.crmapi.sistemacrm.controller;

import com.crmapi.sistemacrm.dto.usuario.UsuarioCreateDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioResponseDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioStatusDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioUpdateDTO;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import com.crmapi.sistemacrm.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    // 1) POST /api/usuarios - @RequestBody -> 201 Created
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar(@Valid @RequestBody UsuarioCreateDTO dto) {
        UsuarioResponseDTO criado = usuarioService.criar(dto);
        URI location = URI.create("/api/usuarios/" + criado.id());
        return ResponseEntity.created(location).body(criado);
    }

    // 2) GET /api/usuarios?busca=&role=&ativo=&page=&size= - @RequestParam -> 200 OK
    @GetMapping
    public ResponseEntity<Page<UsuarioResponseDTO>> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) UsuarioRole role,
            @RequestParam(required = false) Boolean ativo,
            Pageable pageable) {
        Page<UsuarioResponseDTO> pagina = usuarioService.listar(busca, role, ativo, pageable);
        return ResponseEntity.ok(pagina);
    }

    // 3) GET /api/usuarios/{id} - @PathVariable -> 200 OK
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    // 4) PUT /api/usuarios/{id} - @PathVariable + @RequestBody -> 200 OK
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizar(@PathVariable Long id,
                                                          @Valid @RequestBody UsuarioUpdateDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizar(id, dto));
    }

    // 5) PATCH /api/usuarios/{id}/status - @PathVariable + @RequestBody -> 200 OK
    @PatchMapping("/{id}/status")
    public ResponseEntity<UsuarioResponseDTO> atualizarStatus(@PathVariable Long id,
                                                                @Valid @RequestBody UsuarioStatusDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizarStatus(id, dto));
    }

    // 6) DELETE /api/usuarios/{id} - @PathVariable -> 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        usuarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
