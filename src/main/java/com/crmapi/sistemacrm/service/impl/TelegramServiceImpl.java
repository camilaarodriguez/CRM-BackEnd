package com.crmapi.sistemacrm.service.impl;

import com.crmapi.sistemacrm.client.TelegramClient;
import com.crmapi.sistemacrm.client.TelegramMensagemRequest;
import com.crmapi.sistemacrm.service.TelegramService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TelegramServiceImpl implements TelegramService {

    private final TelegramClient telegramClient;
    private final String token;
    private final String chatId;

    public TelegramServiceImpl(TelegramClient telegramClient,
                               @Value("${telegram.bot.token}") String token,
                               @Value("${telegram.bot.chat-id}") String chatId) {
        this.telegramClient = telegramClient;
        this.token = token;
        this.chatId = chatId;
    }

    @Override
    public void notificar(String mensagem) {
        try {
            telegramClient.enviarMensagem(token, new TelegramMensagemRequest(chatId, mensagem));
            log.info("Notificacao enviada ao Telegram: {}", mensagem);
        } catch (Exception e) {
            log.error("Falha ao enviar notificacao ao Telegram: {}", e.getMessage());
        }
    }
}