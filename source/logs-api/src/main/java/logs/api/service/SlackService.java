package logs.api.service;

import logs.api.constant.BaseConstant;
import logs.api.service.feign.FeignConst;
import logs.api.service.feign.FeignSlackService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SlackService {
    @Autowired
    private FeignSlackService feignSlackService;

    public void sendMessage(String token, String channel, String message) {
        if (StringUtils.isBlank(token)) {
            log.error("Missing Slack bot token, skip sending message");
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("channel", channel);
        payload.put("text", message);
        String bearerToken = BaseConstant.AUTH_BEARER_TOKEN + token;
        try {
            Map<String, Object> response = feignSlackService.postMessage(bearerToken, FeignConst.LOGIN_TYPE_NO_AUTH, payload);
            log.info("Slack response: {}", response);
        } catch (Exception e) {
            log.error("Error sending Slack message: {}", e.getMessage(), e);
            throw new RuntimeException("Error sending Slack message", e);
        }
    }
}
