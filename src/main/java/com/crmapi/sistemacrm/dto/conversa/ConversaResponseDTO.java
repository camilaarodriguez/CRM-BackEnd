package com.crmapi.sistemacrm.dto.conversa;

import com.crmapi.sistemacrm.model.enums.StatusConversa;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "ConversaResponse", description = "Conversa de atendimento devolvida pela API")
public record ConversaResponseDTO(

        @Schema(description = "Identificador da conversa", example = "1")
        Long id,

        @Schema(description = "Identificador do cliente atendido", example = "1")
        Long clienteId,

        @Schema(description = "Nome do cliente atendido", example = "Mariana Alves Souza")
        String clienteNome,

        @Schema(description = "Identificador do vendedor responsavel", example = "3")
        Long vendedorId,

        @Schema(description = "Nome do vendedor responsavel", example = "Ana Beatriz Moraes")
        String vendedorNome,

        @Schema(description = "Situacao da conversa", example = "EM_ATENDIMENTO")
        StatusConversa status,

        @Schema(description = "Quantidade de mensagens recebidas ainda nao lidas", example = "2")
        Integer naoLidas,

        @Schema(description = "Data e hora da ultima mensagem trocada", example = "2026-09-17T18:05:00")
        LocalDateTime ultimaMensagemEm,

        @Schema(description = "Limite da janela de atendimento do canal externo", example = "2026-09-18T18:05:00")
        LocalDateTime janelaExpiraEm,

        @Schema(description = "Data e hora da abertura da conversa", example = "2026-09-05T09:00:00")
        LocalDateTime criadoEm,

        @Schema(description = "Data e hora da ultima alteracao", example = "2026-09-17T18:05:00")
        LocalDateTime atualizadoEm
) {
}
