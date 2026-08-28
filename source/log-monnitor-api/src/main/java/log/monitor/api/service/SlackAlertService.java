package log.monitor.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import log.monitor.api.constant.BaseConstant;
import log.monitor.api.model.Setting;
import log.monitor.api.repository.SettingRepository;
import log.monitor.api.service.feign.FeignConst;
import log.monitor.api.service.feign.FeignSlackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class SlackAlertService {

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private FeignSlackService feignSlackService;

    @Autowired
    private ObjectMapper objectMapper;

    public void sendMessage(String title, List<String> lines) {
        Optional<Setting> settingOpt = settingRepository.findFirstByGroupNameAndKeyName(
                BaseConstant.SETTING_GROUP_NOTIFICATION, BaseConstant.SETTING_KEY_SLACK_ERROR_ALERT);
        if (!settingOpt.isPresent()) {
            log.error("Setting [{}/{}] not found, skip sending Slack alert",
                    BaseConstant.SETTING_GROUP_NOTIFICATION, BaseConstant.SETTING_KEY_SLACK_ERROR_ALERT);
            return;
        }

        try {
            JsonNode config = objectMapper.readTree(settingOpt.get().getValueData());
            String channel = config.path("channel").asText(null);
            String token = config.path("token").asText(null);
            if (channel == null || token == null) {
                log.error("Setting [{}/{}] is missing channel/token, skip sending Slack alert",
                        BaseConstant.SETTING_GROUP_NOTIFICATION, BaseConstant.SETTING_KEY_SLACK_ERROR_ALERT);
                return;
            }

            Map<String, Object> payload = buildPayload(channel, title, lines);
            Map<String, Object> response = feignSlackService.postMessage(
                    BaseConstant.AUTH_BEARER_TOKEN + token, FeignConst.LOGIN_TYPE_NO_AUTH, payload);
            if (response != null && Boolean.FALSE.equals(response.get("ok"))) {
                log.error("Slack API rejected the message: {}", response.get("error"));
            }
        } catch (Exception e) {
            log.error("Failed to send Slack alert", e);
        }
    }

    private Map<String, Object> buildPayload(String channel, String title, List<String> lines) {
        List<Map<String, Object>> blocks = new ArrayList<>();
        blocks.add(headerBlock(title));
        blocks.add(dividerBlock());
        for (String line : lines) {
            blocks.add(sectionBlock(line));
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("channel", channel);
        payload.put("text", title);
        payload.put("blocks", blocks);
        return payload;
    }

    private Map<String, Object> headerBlock(String text) {
        Map<String, Object> block = new HashMap<>();
        block.put("type", "header");
        Map<String, Object> textNode = new HashMap<>();
        textNode.put("type", "plain_text");
        textNode.put("text", text);
        block.put("text", textNode);
        return block;
    }

    private Map<String, Object> dividerBlock() {
        Map<String, Object> block = new HashMap<>();
        block.put("type", "divider");
        return block;
    }

    private Map<String, Object> sectionBlock(String markdown) {
        Map<String, Object> block = new HashMap<>();
        block.put("type", "section");
        Map<String, Object> textNode = new HashMap<>();
        textNode.put("type", "mrkdwn");
        textNode.put("text", markdown);
        block.put("text", textNode);
        return block;
    }
}
