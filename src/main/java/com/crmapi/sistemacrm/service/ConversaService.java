package com.crmapi.sistemacrm.service;

import com.crmapi.sistemacrm.dto.conversa.ConversaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConversaService {

    Page<ConversaResponseDTO> listar(Long vendedorId, Pageable pageable);

    ConversaResponseDTO buscarPorId(Long id);
}