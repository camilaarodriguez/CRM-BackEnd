package com.crmapi.sistemacrm.dto.mensagem;

import com.crmapi.sistemacrm.model.enums.TipoMensagem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "MensagemCreate", description = "Mensagem enviada pelo vendedor dentro de uma conversa")
public record MensagemCreateDTO(

        @Schema(description = "Identificador da conversa de destino", example = "1")
        @NotNull(message = "O conversaId e obrigatorio")
        Long conversaId,

        @Schema(description = "Formato do conteudo enviado", example = "TEXTO")
        @NotNull(message = "O tipo e obrigatorio")
        TipoMensagem tipo,

        @Schema(description = "Conteudo da mensagem",
                example = "Ola, Mariana! Consegui liberar a condicao especial para 12 licencas.")
        @NotBlank(message = "O conteudo e obrigatorio")
        String conteudo
) {
}
