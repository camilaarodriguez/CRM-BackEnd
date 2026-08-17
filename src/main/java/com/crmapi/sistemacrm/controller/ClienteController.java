package com.crmapi.sistemacrm.controller;

import com.crmapi.sistemacrm.dto.cliente.ClienteCreateDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteResponseDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteStatusFunilDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteUpdateDTO;
import com.crmapi.sistemacrm.model.enums.StatusFunil;
import com.crmapi.sistemacrm.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    // 7) POST /api/clientes - @RequestBody -> 201 Created
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> criar(@Valid @RequestBody ClienteCreateDTO dto) {
        ClienteResponseDTO criado = clienteService.criar(dto);
        URI location = URI.create("/api/clientes/" + criado.id());
        return ResponseEntity.created(location).body(criado);
    }

    // 8) GET /api/clientes?busca=&status=&vendedorId=&incluirInativos=&page=&size= - @RequestParam -> 200 OK
    @GetMapping
    public ResponseEntity<Page<ClienteResponseDTO>> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) StatusFunil status,
            @RequestParam(required = false) Long vendedorId,
            @RequestParam(required = false) Boolean incluirInativos,
            Pageable pageable) {
        Page<ClienteResponseDTO> pagina = clienteService.listar(busca, status, vendedorId, incluirInativos, pageable);
        return ResponseEntity.ok(pagina);
    }

    // 9) GET /api/clientes/{id} - @PathVariable -> 200 OK
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    // 10) PUT /api/clientes/{id} - @PathVariable + @RequestBody -> 200 OK
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> atualizar(@PathVariable Long id,
                                                          @Valid @RequestBody ClienteUpdateDTO dto) {
        return ResponseEntity.ok(clienteService.atualizar(id, dto));
    }

    // 11) PATCH /api/clientes/{id}/status-funil - @PathVariable + @RequestBody -> 200 OK
    @PatchMapping("/{id}/status-funil")
    public ResponseEntity<ClienteResponseDTO> atualizarStatusFunil(@PathVariable Long id,
                                                                     @Valid @RequestBody ClienteStatusFunilDTO dto) {
        return ResponseEntity.ok(clienteService.atualizarStatusFunil(id, dto));
    }

    // 12) DELETE /api/clientes/{id} - @PathVariable -> 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        clienteService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
