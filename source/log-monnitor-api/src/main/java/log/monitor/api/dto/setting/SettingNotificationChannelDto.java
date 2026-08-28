package log.monitor.api.dto.setting;

import lombok.Data;

@Data
public class SettingNotificationChannelDto {
    private Integer type;
    private String channel;
    private String token;
    private String username;
}
