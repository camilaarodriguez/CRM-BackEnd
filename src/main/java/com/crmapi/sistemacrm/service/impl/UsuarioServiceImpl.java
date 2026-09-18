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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    @Override
    public UsuarioResponseDTO criar(UsuarioCreateDTO dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            log.warn("Tentativa de criar usuario com email ja cadastrado: {}", dto.email());
            throw new BusinessException("Ja existe um usuario cadastrado com o email informado");
        }
        try {
            Usuario usuario = usuarioMapper.paraEntidade(dto);
            Usuario salvo = usuarioRepository.save(usuario);
            log.info("Usuario criado: id={}, email={}, role={}", salvo.getId(), salvo.getEmail(), salvo.getRole());
            return usuarioMapper.paraResponseDTO(salvo);
        } catch (Exception e) {
            log.error("Erro ao criar usuario com email {}: {}", dto.email(), e.getMessage());
            throw new BusinessException("Nao foi possivel criar o usuario");
        }
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

        try {
            usuario.setNome(dto.nome());
            usuario.setEmail(dto.email());
            usuario.setRole(dto.role());

            Usuario atualizado = usuarioRepository.save(usuario);
            log.info("Usuario atualizado: id={}, email={}", id, dto.email());
            return usuarioMapper.paraResponseDTO(atualizado);
        } catch (Exception e) {
            log.error("Erro ao atualizar usuario id={}: {}", id, e.getMessage());
            throw new BusinessException("Nao foi possivel atualizar o usuario");
        }
    }

    @Override
    public UsuarioResponseDTO atualizarStatus(Long id, UsuarioStatusDTO dto) {
        Usuario usuario = buscarEntidadePorId(id);
        try {
            usuario.setAtivo(dto.ativo());
            Usuario atualizado = usuarioRepository.save(usuario);
            log.info("Status do usuario atualizado: id={}, ativo={}", id, dto.ativo());
            return usuarioMapper.paraResponseDTO(atualizado);
        } catch (Exception e) {
            log.error("Erro ao atualizar status do usuario id={}: {}", id, e.getMessage());
            throw new BusinessException("Nao foi possivel atualizar o status do usuario");
        }
    }

    @Override
    public void deletar(Long id) {
        Usuario usuario = buscarEntidadePorId(id);
        try {
            usuarioRepository.delete(usuario);
            log.info("Usuario deletado: id={}", id);
        } catch (Exception e) {
            log.error("Erro ao deletar usuario id={}: {}", id, e.getMessage());
            throw new BusinessException("Nao foi possivel deletar o usuario");
        }
    }

    private Usuario buscarEntidadePorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com o id: " + id));
    }
}