package logs.api.dto.setting;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingNotificationChannelDto {
    private Integer type;
    private String channel;
    private String token;
    private String username;
}
