package com.crmapi.sistemacrm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "telegramClient", url = "https://api.telegram.org")
public interface TelegramClient {

    @PostMapping("/bot{token}/sendMessage")
    void enviarMensagem(@org.springframework.web.bind.annotation.PathVariable("token") String token,
                        @RequestBody TelegramMensagemRequest request);
}