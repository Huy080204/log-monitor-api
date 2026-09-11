package logs.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import logs.api.constant.BaseConstant;
import logs.api.dto.setting.SettingNotificationChannelDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NotificationService {

    private static final String ELLIPSIS = "...";

    @Autowired
    private ObjectMapper objectMapper;

    public SettingNotificationChannelDto parseChannelSetting(String channelSetting) {
        if (StringUtils.isBlank(channelSetting)) {
            return new SettingNotificationChannelDto();
        }
        try {
            return objectMapper.readValue(channelSetting, SettingNotificationChannelDto.class);
        } catch (Exception e) {
            log.error("Failed to parse channel setting JSON: {}", e.getMessage());
            return new SettingNotificationChannelDto();
        }
    }

    // Max characters allowed per channel
    public int messageBudget(Integer channelType) {
        return BaseConstant.NOTIFICATION_CHANNEL_TYPE_TELEGRAM.equals(channelType)
                ? BaseConstant.NOTIFICATION_MESSAGE_MAX_LENGTH_TELEGRAM
                : BaseConstant.NOTIFICATION_MESSAGE_MAX_LENGTH_SLACK;
    }

    public String truncate(String value, int maxLength) {
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return maxLength <= ELLIPSIS.length() ? value.substring(0, maxLength)
                : value.substring(0, maxLength - ELLIPSIS.length()) + ELLIPSIS;
    }

    // Split one app's breach lines into chunks only if they alone exceed the limit
    public List<String> buildAppChunks(String app, List<String> breachLines, int budget) {
        String header = String.format("- %s", app);
        int headerLength = header.length();

        List<String> chunks = new ArrayList<>();
        StringBuilder chunk = new StringBuilder(header);
        for (String breachLine : breachLines) {
            String line = truncate(breachLine, budget - headerLength - 1);
            if (chunk.length() > headerLength && chunk.length() + 1 + line.length() > budget) {
                chunks.add(chunk.toString());
                chunk = new StringBuilder(header);
            }
            chunk.append("\n").append(line);
        }
        chunks.add(chunk.toString());
        return chunks;
    }

    // Pack app chunks into messages, starting a new message once the limit is hit
    public List<String> packBodies(List<String> apps, Map<String, List<String>> breachLinesByApp, int budget) {
        List<String> bodies = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String app : apps) {
            for (String chunk : buildAppChunks(app, breachLinesByApp.get(app), budget)) {
                if (current.length() > 0 && current.length() + 1 + chunk.length() > budget) {
                    bodies.add(current.toString());
                    current = new StringBuilder();
                }
                if (current.length() > 0) {
                    current.append("\n");
                }
                current.append(chunk);
            }
        }
        if (current.length() > 0) {
            bodies.add(current.toString());
        }
        return bodies;
    }
}
