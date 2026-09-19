package com.crmapi.sistemacrm.service.impl;

import com.crmapi.sistemacrm.dto.conversa.ConversaResponseDTO;
import com.crmapi.sistemacrm.exception.BusinessException;
import com.crmapi.sistemacrm.exception.ResourceNotFoundException;
import com.crmapi.sistemacrm.mapper.ConversaMapper;
import com.crmapi.sistemacrm.model.Conversa;
import com.crmapi.sistemacrm.repository.ConversaRepository;
import com.crmapi.sistemacrm.service.ConversaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConversaServiceImpl implements ConversaService {

    private final ConversaRepository conversaRepository;
    private final ConversaMapper conversaMapper;

    @Override
    public Page<ConversaResponseDTO> listar(Long vendedorId, Pageable pageable) {
        try {
            Page<Conversa> pagina = (vendedorId != null)
                    ? conversaRepository.findByVendedorId(vendedorId, pageable)
                    : conversaRepository.findAll(pageable);
            return pagina.map(conversaMapper::paraResponseDTO);
        } catch (Exception e) {
            log.error("Erro ao listar conversas: {}", e.getMessage());
            throw new BusinessException("Nao foi possivel listar as conversas");
        }
    }

    @Override
    public ConversaResponseDTO buscarPorId(Long id) {
        try {
            Conversa conversa = conversaRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversa nao encontrada com o id: " + id));
            return conversaMapper.paraResponseDTO(conversa);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao buscar conversa id={}: {}", id, e.getMessage());
            throw new BusinessException("Nao foi possivel buscar a conversa");
        }
    }
}