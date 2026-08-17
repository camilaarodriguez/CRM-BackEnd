package com.crmapi.sistemacrm.service.impl;

import com.crmapi.sistemacrm.dto.usuario.UsuarioCreateDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioResponseDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioStatusDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioUpdateDTO;
import com.crmapi.sistemacrm.exception.BusinessException;
import com.crmapi.sistemacrm.exception.ResourceNotFoundException;
import com.crmapi.sistemacrm.mapper.UsuarioMapper;
import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import com.crmapi.sistemacrm.repository.UsuarioRepository;
import com.crmapi.sistemacrm.repository.specification.UsuarioSpecification;
import com.crmapi.sistemacrm.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    @Override
    public UsuarioResponseDTO criar(UsuarioCreateDTO dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new BusinessException("Ja existe um usuario cadastrado com o email informado");
        }
        Usuario usuario = usuarioMapper.paraEntidade(dto);
        Usuario salvo = usuarioRepository.save(usuario);
        return usuarioMapper.paraResponseDTO(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponseDTO> listar(String busca, UsuarioRole role, Boolean ativo, Pageable pageable) {
        return usuarioRepository.findAll(UsuarioSpecification.comFiltros(busca, role, ativo), pageable)
                .map(usuarioMapper::paraResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id) {
        Usuario usuario = buscarEntidadePorId(id);
        return usuarioMapper.paraResponseDTO(usuario);
    }

    @Override
    public UsuarioResponseDTO atualizar(Long id, UsuarioUpdateDTO dto) {
        Usuario usuario = buscarEntidadePorId(id);

        usuarioRepository.findByEmail(dto.email()).ifPresent(existente -> {
            if (!existente.getId().equals(id)) {
                throw new BusinessException("Ja existe um usuario cadastrado com o email informado");
            }
        });

        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setRole(dto.role());

        Usuario atualizado = usuarioRepository.save(usuario);
        return usuarioMapper.paraResponseDTO(atualizado);
    }

    @Override
    public UsuarioResponseDTO atualizarStatus(Long id, UsuarioStatusDTO dto) {
        Usuario usuario = buscarEntidadePorId(id);
        usuario.setAtivo(dto.ativo());
        Usuario atualizado = usuarioRepository.save(usuario);
        return usuarioMapper.paraResponseDTO(atualizado);
    }

    @Override
    public void deletar(Long id) {
        Usuario usuario = buscarEntidadePorId(id);
        usuarioRepository.delete(usuario);
    }

    private Usuario buscarEntidadePorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com o id: " + id));
    }
}
