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

    // Escape the 3 characters that break both Telegram HTML and Slack mrkdwn markup
    public String escapeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // Wrap a display name as a clickable link in the target channel's own markup syntax
    public String formatLink(Integer channelType, String url, String text) {
        String safeText = escapeText(text);
        if (BaseConstant.NOTIFICATION_CHANNEL_TYPE_TELEGRAM.equals(channelType)) {
            return String.format("<a href=\"%s\">%s</a>", url, safeText);
        }
        return String.format("<%s|%s>", url, safeText);
    }

    // Split one header's lines into chunks if they alone exceed the limit; `header` must already be display-ready (escaped or linked)
    public List<String> buildHeaderChunks(String header, List<String> lines, int budget) {
        String headerLine = String.format("- %s", header);
        int headerLength = headerLine.length();

        List<String> chunks = new ArrayList<>();
        StringBuilder chunk = new StringBuilder(headerLine);
        for (String rawLine : lines) {
            String line = truncate(rawLine, budget - headerLength - 1);
            if (chunk.length() > headerLength && chunk.length() + 1 + line.length() > budget) {
                chunks.add(chunk.toString());
                chunk = new StringBuilder(headerLine);
            }
            chunk.append("\n").append(line);
        }
        chunks.add(chunk.toString());
        return chunks;
    }

    // Pack header chunks into messages, starting a new message once the limit is hit
    public List<String> packBodies(List<String> headers, Map<String, List<String>> linesByHeader, int budget) {
        List<String> bodies = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String header : headers) {
            for (String chunk : buildHeaderChunks(header, linesByHeader.get(header), budget)) {
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
