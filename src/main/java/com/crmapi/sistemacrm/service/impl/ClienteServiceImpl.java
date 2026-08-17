package com.crmapi.sistemacrm.service.impl;

import com.crmapi.sistemacrm.dto.cliente.ClienteCreateDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteResponseDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteStatusFunilDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteUpdateDTO;
import com.crmapi.sistemacrm.exception.ResourceNotFoundException;
import com.crmapi.sistemacrm.mapper.ClienteMapper;
import com.crmapi.sistemacrm.model.Cliente;
import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.StatusFunil;
import com.crmapi.sistemacrm.repository.ClienteRepository;
import com.crmapi.sistemacrm.repository.UsuarioRepository;
import com.crmapi.sistemacrm.repository.specification.ClienteSpecification;
import com.crmapi.sistemacrm.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteMapper clienteMapper;

    @Override
    public ClienteResponseDTO criar(ClienteCreateDTO dto) {
        Usuario vendedor = buscarVendedorPorId(dto.vendedorId());

        Cliente cliente = Cliente.builder()
                .nome(dto.nome())
                .email(dto.email())
                .telefone(dto.telefone())
                .vendedor(vendedor)
                .statusFunil(dto.statusFunil() != null ? dto.statusFunil() : StatusFunil.NOVO)
                .ativo(true)
                .build();

        Cliente salvo = clienteRepository.save(cliente);
        return clienteMapper.paraResponseDTO(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> listar(String busca, StatusFunil status, Long vendedorId,
                                            Boolean incluirInativos, Pageable pageable) {
        return clienteRepository
                .findAll(ClienteSpecification.comFiltros(busca, status, vendedorId, incluirInativos), pageable)
                .map(clienteMapper::paraResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long id) {
        Cliente cliente = buscarEntidadePorId(id);
        return clienteMapper.paraResponseDTO(cliente);
    }

    @Override
    public ClienteResponseDTO atualizar(Long id, ClienteUpdateDTO dto) {
        Cliente cliente = buscarEntidadePorId(id);
        Usuario vendedor = buscarVendedorPorId(dto.vendedorId());

        cliente.setNome(dto.nome());
        cliente.setEmail(dto.email());
        cliente.setTelefone(dto.telefone());
        cliente.setVendedor(vendedor);

        Cliente atualizado = clienteRepository.save(cliente);
        return clienteMapper.paraResponseDTO(atualizado);
    }

    @Override
    public ClienteResponseDTO atualizarStatusFunil(Long id, ClienteStatusFunilDTO dto) {
        Cliente cliente = buscarEntidadePorId(id);
        cliente.setStatusFunil(dto.statusFunil());
        Cliente atualizado = clienteRepository.save(cliente);
        return clienteMapper.paraResponseDTO(atualizado);
    }

    @Override
    public void deletar(Long id) {
        Cliente cliente = buscarEntidadePorId(id);
        clienteRepository.delete(cliente);
    }

    private Cliente buscarEntidadePorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado com o id: " + id));
    }

    private Usuario buscarVendedorPorId(Long vendedorId) {
        return usuarioRepository.findById(vendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor (usuario) nao encontrado com o id: " + vendedorId));
    }
}
