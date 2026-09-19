package com.crmapi.sistemacrm.service.impl;

import com.crmapi.sistemacrm.dto.mensagem.MensagemCreateDTO;
import com.crmapi.sistemacrm.dto.mensagem.MensagemResponseDTO;
import com.crmapi.sistemacrm.exception.BusinessException;
import com.crmapi.sistemacrm.exception.ResourceNotFoundException;
import com.crmapi.sistemacrm.mapper.MensagemMapper;
import com.crmapi.sistemacrm.model.Conversa;
import com.crmapi.sistemacrm.model.Mensagem;
import com.crmapi.sistemacrm.model.enums.DirecaoMensagem;
import com.crmapi.sistemacrm.model.enums.StatusConversa;
import com.crmapi.sistemacrm.model.enums.StatusEntrega;
import com.crmapi.sistemacrm.repository.ConversaRepository;
import com.crmapi.sistemacrm.repository.MensagemRepository;
import com.crmapi.sistemacrm.service.MensagemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MensagemServiceImpl implements MensagemService {

    private final MensagemRepository mensagemRepository;
    private final ConversaRepository conversaRepository;
    private final MensagemMapper mensagemMapper;

    @Override
    public MensagemResponseDTO enviar(MensagemCreateDTO dto) {
        Conversa conversa = conversaRepository.findById(dto.conversaId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversa nao encontrada com o id: " + dto.conversaId()));

        try {
            Mensagem mensagem = Mensagem.builder()
                    .conversa(conversa)
                    .direcao(DirecaoMensagem.SAIDA)
                    .tipo(dto.tipo())
                    .conteudo(dto.conteudo())
                    .statusEntrega(StatusEntrega.ENVIADA)
                    .enviadaPor(conversa.getVendedor())
                    .build();

            Mensagem salva = mensagemRepository.save(mensagem);

            conversa.setUltimaMensagemEm(LocalDateTime.now());
            // A primeira resposta da equipe tira a conversa da fila e a poe em atendimento.
            if (conversa.getStatus() == StatusConversa.ABERTA) {
                conversa.setStatus(StatusConversa.EM_ATENDIMENTO);
            }
            conversaRepository.save(conversa);

            log.info("Mensagem enviada: id={}, conversaId={}, tipo={}", salva.getId(), conversa.getId(), dto.tipo());

            return mensagemMapper.paraResponseDTO(salva);
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem na conversa id={}: {}", dto.conversaId(), e.getMessage());
            throw new BusinessException("Nao foi possivel enviar a mensagem");
        }
    }

    @Override
    public List<MensagemResponseDTO> listarPorConversa(Long conversaId) {
        Conversa conversa = conversaRepository.findById(conversaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversa nao encontrada com o id: " + conversaId));

        // Abrir o historico marca como lidas as mensagens recebidas do cliente.
        if (conversa.getNaoLidas() != null && conversa.getNaoLidas() > 0) {
            conversa.setNaoLidas(0);
            conversaRepository.save(conversa);
            log.info("Conversa marcada como lida: id={}", conversaId);
        }

        return mensagemRepository.findByConversaIdOrderByCriadoEmAsc(conversaId).stream()
                .map(mensagemMapper::paraResponseDTO)
                .toList();
    }
}