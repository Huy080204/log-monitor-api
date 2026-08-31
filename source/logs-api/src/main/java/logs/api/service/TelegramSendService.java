package logs.api.service;

import logs.api.service.feign.FeignConst;
import logs.api.service.feign.FeignTelegramService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class TelegramSendService {
    @Autowired
    private FeignTelegramService feignTelegramService;

    public void sendMessage(String token, String chatId, String text) {
        if (StringUtils.isBlank(token)) {
            log.error("Missing Telegram bot token, skip sending message");
            return;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("chat_id", chatId);
            payload.put("text", text);
            String response = feignTelegramService.sendMessage(token, FeignConst.LOGIN_TYPE_NO_AUTH, payload);
            log.info("Telegram sendMessage response: {}", response);
        } catch (Exception e) {
            log.error("Failed to send message via Telegram: {}", e.getMessage(), e);
        }
    }
}
