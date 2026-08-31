package logs.api.dto.notificationGroup;

import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.dto.ABasicAdminDto;
import lombok.Data;

@Data
@Schema
public class NotificationGroupDto extends ABasicAdminDto {
    @Schema(name = "name")
    private String name;
    @Schema(name = "description")
    private String description;
    @Schema(name = "channelSetting")
    private String channelSetting;
    @Schema(name = "type")
    private Integer type;
}
