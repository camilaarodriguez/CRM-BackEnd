package com.crmapi.sistemacrm.dto.mensagem;

import com.crmapi.sistemacrm.model.enums.DirecaoMensagem;
import com.crmapi.sistemacrm.model.enums.StatusEntrega;
import com.crmapi.sistemacrm.model.enums.TipoMensagem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "MensagemResponse", description = "Mensagem do historico de uma conversa")
public record MensagemResponseDTO(

        @Schema(description = "Identificador da mensagem", example = "1")
        Long id,

        @Schema(description = "Identificador da conversa", example = "1")
        Long conversaId,

        @Schema(description = "ENTRADA para mensagem recebida do cliente, SAIDA para enviada pela equipe",
                example = "SAIDA")
        DirecaoMensagem direcao,

        @Schema(description = "Formato do conteudo", example = "TEXTO")
        TipoMensagem tipo,

        @Schema(description = "Conteudo da mensagem",
                example = "Ola, Mariana! Consegui liberar a condicao especial para 12 licencas.")
        String conteudo,

        @Schema(description = "Identificador da mensagem no canal externo", example = "wamid.0002")
        String waMessageId,

        @Schema(description = "Situacao de entrega no canal externo", example = "ENVIADA")
        StatusEntrega statusEntrega,

        @Schema(description = "Identificador do usuario que enviou, nulo em mensagens recebidas", example = "3")
        Long enviadaPorId,

        @Schema(description = "Nome do usuario que enviou, nulo em mensagens recebidas",
                example = "Ana Beatriz Moraes")
        String enviadaPorNome,

        @Schema(description = "Data e hora do registro da mensagem", example = "2026-09-17T18:05:00")
        LocalDateTime criadoEm
) {
}
