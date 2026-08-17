package com.crmapi.sistemacrm.service;

import com.crmapi.sistemacrm.dto.cliente.ClienteCreateDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteResponseDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteStatusFunilDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteUpdateDTO;
import com.crmapi.sistemacrm.model.enums.StatusFunil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClienteService {

    ClienteResponseDTO criar(ClienteCreateDTO dto);

    Page<ClienteResponseDTO> listar(String busca, StatusFunil status, Long vendedorId, Boolean incluirInativos,
                                     Pageable pageable);

    ClienteResponseDTO buscarPorId(Long id);

    ClienteResponseDTO atualizar(Long id, ClienteUpdateDTO dto);

    ClienteResponseDTO atualizarStatusFunil(Long id, ClienteStatusFunilDTO dto);

    void deletar(Long id);
}
