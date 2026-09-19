package com.crmapi.sistemacrm.service;

import com.crmapi.sistemacrm.dto.mensagem.MensagemCreateDTO;
import com.crmapi.sistemacrm.dto.mensagem.MensagemResponseDTO;

import java.util.List;

public interface MensagemService {

    MensagemResponseDTO enviar(MensagemCreateDTO dto);

    List<MensagemResponseDTO> listarPorConversa(Long conversaId);
}