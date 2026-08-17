package com.crmapi.sistemacrm.service;

import com.crmapi.sistemacrm.dto.usuario.UsuarioCreateDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioResponseDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioStatusDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioUpdateDTO;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {

    UsuarioResponseDTO criar(UsuarioCreateDTO dto);

    Page<UsuarioResponseDTO> listar(String busca, UsuarioRole role, Boolean ativo, Pageable pageable);

    UsuarioResponseDTO buscarPorId(Long id);

    UsuarioResponseDTO atualizar(Long id, UsuarioUpdateDTO dto);

    UsuarioResponseDTO atualizarStatus(Long id, UsuarioStatusDTO dto);

    void deletar(Long id);
}
