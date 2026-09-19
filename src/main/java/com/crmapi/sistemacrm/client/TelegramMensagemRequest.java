package com.crmapi.sistemacrm.client;

public record TelegramMensagemRequest(
        String chat_id,
        String text
) {
}