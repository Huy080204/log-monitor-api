package logs.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import logs.api.dto.setting.SettingNotificationChannelDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {
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
}
