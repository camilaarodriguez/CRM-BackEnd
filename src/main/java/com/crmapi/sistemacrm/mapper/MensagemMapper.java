package com.crmapi.sistemacrm.mapper;

import com.crmapi.sistemacrm.dto.mensagem.MensagemResponseDTO;
import com.crmapi.sistemacrm.model.Mensagem;
import org.springframework.stereotype.Component;

@Component
public class MensagemMapper {

    public MensagemResponseDTO paraResponseDTO(Mensagem mensagem) {
        return new MensagemResponseDTO(
                mensagem.getId(),
                mensagem.getConversa() != null ? mensagem.getConversa().getId() : null,
                mensagem.getDirecao(),
                mensagem.getTipo(),
                mensagem.getConteudo(),
                mensagem.getWaMessageId(),
                mensagem.getStatusEntrega(),
                mensagem.getEnviadaPor() != null ? mensagem.getEnviadaPor().getId() : null,
                mensagem.getEnviadaPor() != null ? mensagem.getEnviadaPor().getNome() : null,
                mensagem.getCriadoEm()
        );
    }
}