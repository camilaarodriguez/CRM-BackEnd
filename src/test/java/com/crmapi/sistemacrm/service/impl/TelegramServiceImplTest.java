package com.crmapi.sistemacrm.service.impl;

import com.crmapi.sistemacrm.client.TelegramClient;
import com.crmapi.sistemacrm.client.TelegramMensagemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Notificacao externa ao Telegram, com o client Feign mockado: nenhum teste faz
 * chamada de rede. O ponto sensivel e o segundo caso - um aviso que falha nao
 * pode derrubar a operacao que o disparou.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TelegramServiceImpl: aviso externo")
class TelegramServiceImplTest {

    @Mock
    private TelegramClient telegramClient;

    private TelegramServiceImpl telegramService;

    @BeforeEach
    void setUp() {
        telegramService = new TelegramServiceImpl(telegramClient, "token-teste", "chat-teste");
    }

    @Test
    @DisplayName("envia a mensagem com o token e o chat configurados")
    void deveEnviarNotificacaoAoTelegram() {
        // Act
        telegramService.notificar("Novo cliente");

        // Assert
        verify(telegramClient).enviarMensagem("token-teste", new TelegramMensagemRequest("chat-teste", "Novo cliente"));
    }

    @Test
    @DisplayName("engole a falha do Telegram para nao derrubar a operacao principal")
    void naoDevePropagarFalhaDoTelegram() {
        // Arrange
        doThrow(new RuntimeException("telegram fora")).when(telegramClient)
                .enviarMensagem(anyString(), any(TelegramMensagemRequest.class));

        // Act + Assert: cadastrar um cliente nao pode falhar porque o aviso caiu.
        assertThatCode(() -> telegramService.notificar("Novo cliente")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("nao chama o Telegram quando o token nao foi configurado")
    void naoDeveChamarOTelegramSemToken() {
        // Arrange
        TelegramServiceImpl semToken = new TelegramServiceImpl(telegramClient, "", "chat-teste");
        TelegramServiceImpl tokenNulo = new TelegramServiceImpl(telegramClient, null, "chat-teste");

        // Act
        semToken.notificar("Novo cliente");
        tokenNulo.notificar("Novo cliente");

        // Assert
        verify(telegramClient, never()).enviarMensagem(anyString(), any(TelegramMensagemRequest.class));
    }
}
