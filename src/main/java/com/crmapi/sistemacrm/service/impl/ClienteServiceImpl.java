package com.crmapi.sistemacrm.service.impl;

import com.crmapi.sistemacrm.dto.cliente.ClienteCreateDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteReatribuirDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteResponseDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteStatusFunilDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteUpdateDTO;
import com.crmapi.sistemacrm.exception.BusinessException;
import com.crmapi.sistemacrm.exception.ResourceNotFoundException;
import com.crmapi.sistemacrm.mapper.ClienteMapper;
import com.crmapi.sistemacrm.model.AtribuicaoLog;
import com.crmapi.sistemacrm.model.Cliente;
import com.crmapi.sistemacrm.model.Conversa;
import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.StatusConversa;
import com.crmapi.sistemacrm.model.enums.StatusFunil;
import com.crmapi.sistemacrm.repository.AtribuicaoLogRepository;
import com.crmapi.sistemacrm.repository.ClienteRepository;
import com.crmapi.sistemacrm.repository.ConversaRepository;
import com.crmapi.sistemacrm.repository.UsuarioRepository;
import com.crmapi.sistemacrm.repository.specification.ClienteSpecification;
import com.crmapi.sistemacrm.service.ClienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.crmapi.sistemacrm.service.TelegramService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConversaRepository conversaRepository;
    private final AtribuicaoLogRepository atribuicaoLogRepository;
    private final ClienteMapper clienteMapper;
    private final TelegramService telegramService;

    @Override
    public ClienteResponseDTO criar(ClienteCreateDTO dto) {
        Usuario vendedor = buscarVendedorPorId(dto.vendedorId());
        validarDuplicidade(dto.telefone(), dto.documento(), null);

        try {
            Cliente cliente = Cliente.builder()
                    .nome(dto.nome())
                    .email(dto.email())
                    .telefone(dto.telefone())
                    .documento(dto.documento())
                    .empresa(dto.empresa())
                    .observacoes(dto.observacoes())
                    .vendedor(vendedor)
                    .statusFunil(dto.statusFunil() != null ? dto.statusFunil() : StatusFunil.NOVO)
                    .ativo(true)
                    .build();

            Cliente salvo = clienteRepository.save(cliente);
            log.info("Cliente criado: id={}, vendedorId={}", salvo.getId(), vendedor.getId());

            criarConversaParaCliente(salvo, vendedor);

            AtribuicaoLog logAtribuicao = AtribuicaoLog.builder()
                    .conversa(conversaRepository.findByClienteId(salvo.getId()).orElseThrow())
                    .deUsuario(null)
                    .paraUsuario(vendedor)
                    .feitaPor(vendedor)
                    .motivo("Atribuicao inicial na criacao do cliente")
                    .build();
            atribuicaoLogRepository.save(logAtribuicao);

            telegramService.notificar("Novo cliente atribuido: " + salvo.getNome() + " -> vendedor " + vendedor.getNome());
            return clienteMapper.paraResponseDTO(salvo);
        } catch (Exception e) {
            log.error("Erro ao criar cliente com vendedorId={}: {}", dto.vendedorId(), e.getMessage());
            throw new BusinessException("Nao foi possivel criar o cliente");
        }
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
        validarDuplicidade(dto.telefone(), dto.documento(), id);

        try {
            cliente.setNome(dto.nome());
            cliente.setEmail(dto.email());
            cliente.setTelefone(dto.telefone());
            cliente.setDocumento(dto.documento());
            cliente.setEmpresa(dto.empresa());
            cliente.setObservacoes(dto.observacoes());
            cliente.setVendedor(vendedor);

            Cliente atualizado = clienteRepository.save(cliente);
            log.info("Cliente atualizado: id={}", id);
            return clienteMapper.paraResponseDTO(atualizado);
        } catch (Exception e) {
            log.error("Erro ao atualizar cliente id={}: {}", id, e.getMessage());
            throw new BusinessException("Nao foi possivel atualizar o cliente");
        }
    }

    @Override
    public ClienteResponseDTO atualizarStatusFunil(Long id, ClienteStatusFunilDTO dto) {
        Cliente cliente = buscarEntidadePorId(id);
        try {
            cliente.setStatusFunil(dto.statusFunil());
            Cliente atualizado = clienteRepository.save(cliente);
            log.info("Status do funil atualizado: clienteId={}, novoStatus={}", id, dto.statusFunil());
            return clienteMapper.paraResponseDTO(atualizado);
        } catch (Exception e) {
            log.error("Erro ao atualizar status do funil do cliente id={}: {}", id, e.getMessage());
            throw new BusinessException("Nao foi possivel atualizar o status do funil");
        }
    }

    @Override
    public ClienteResponseDTO reatribuir(Long id, ClienteReatribuirDTO dto) {
        Cliente cliente = buscarEntidadePorId(id);
        Usuario vendedorAntigo = cliente.getVendedor();
        Usuario vendedorNovo = buscarVendedorPorId(dto.novoVendedorId());

        try {
            cliente.setVendedor(vendedorNovo);
            Cliente atualizado = clienteRepository.save(cliente);

            Conversa conversa = conversaRepository.findByClienteId(cliente.getId())
                    .orElseGet(() -> criarConversaParaCliente(cliente, vendedorNovo));
            conversa.setVendedor(vendedorNovo);
            conversaRepository.save(conversa);

            AtribuicaoLog logAtribuicao = AtribuicaoLog.builder()
                    .conversa(conversa)
                    .deUsuario(vendedorAntigo)
                    .paraUsuario(vendedorNovo)
                    .feitaPor(vendedorNovo)
                    .motivo(dto.motivo())
                    .build();
            atribuicaoLogRepository.save(logAtribuicao);
            telegramService.notificar("Cliente reatribuido: " + cliente.getNome() + " -> vendedor " + vendedorNovo.getNome());

            log.info("Cliente reatribuido: clienteId={}, de={}, para={}", id,
                    vendedorAntigo != null ? vendedorAntigo.getId() : null, vendedorNovo.getId());

            return clienteMapper.paraResponseDTO(atualizado);
        } catch (Exception e) {
            log.error("Erro ao reatribuir cliente id={}: {}", id, e.getMessage());
            throw new BusinessException("Nao foi possivel reatribuir o cliente");
        }
    }

    @Override
    public void deletar(Long id) {
        Cliente cliente = buscarEntidadePorId(id);

        if (Boolean.FALSE.equals(cliente.getAtivo())) {
            throw new BusinessException("Este cliente ja esta inativo");
        }

        // Exclusao logica: o cliente sai da carteira, mas a conversa e o historico
        // de mensagens continuam disponiveis para consulta.
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
        log.info("Cliente inativado: id={}", id);
    }

    private void validarDuplicidade(String telefone, String documento, Long idAtual) {
        boolean telefoneEmUso = (idAtual == null)
                ? clienteRepository.existsByTelefone(telefone)
                : clienteRepository.existsByTelefoneAndIdNot(telefone, idAtual);
        if (telefoneEmUso) {
            throw new BusinessException("Ja existe um cliente cadastrado com o telefone informado");
        }

        if (documento == null || documento.isBlank()) {
            return;
        }

        boolean documentoEmUso = (idAtual == null)
                ? clienteRepository.existsByDocumento(documento)
                : clienteRepository.existsByDocumentoAndIdNot(documento, idAtual);
        if (documentoEmUso) {
            throw new BusinessException("Ja existe um cliente cadastrado com o documento informado");
        }
    }

    private Conversa criarConversaParaCliente(Cliente cliente, Usuario vendedor) {
        Conversa conversa = Conversa.builder()
                .cliente(cliente)
                .vendedor(vendedor)
                .status(StatusConversa.ABERTA)
                .naoLidas(0)
                .build();
        return conversaRepository.save(conversa);
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