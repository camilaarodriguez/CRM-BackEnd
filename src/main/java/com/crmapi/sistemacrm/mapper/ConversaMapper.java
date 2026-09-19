package com.crmapi.sistemacrm.mapper;

import com.crmapi.sistemacrm.dto.conversa.ConversaResponseDTO;
import com.crmapi.sistemacrm.model.Conversa;
import org.springframework.stereotype.Component;

@Component
public class ConversaMapper {

    public ConversaResponseDTO paraResponseDTO(Conversa conversa) {
        return new ConversaResponseDTO(
                conversa.getId(),
                conversa.getCliente() != null ? conversa.getCliente().getId() : null,
                conversa.getCliente() != null ? conversa.getCliente().getNome() : null,
                conversa.getVendedor() != null ? conversa.getVendedor().getId() : null,
                conversa.getVendedor() != null ? conversa.getVendedor().getNome() : null,
                conversa.getStatus(),
                conversa.getNaoLidas(),
                conversa.getUltimaMensagemEm(),
                conversa.getJanelaExpiraEm(),
                conversa.getCriadoEm(),
                conversa.getAtualizadoEm()
        );
    }
}