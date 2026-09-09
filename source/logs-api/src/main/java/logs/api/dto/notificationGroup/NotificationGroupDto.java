package logs.api.dto.notificationGroup;

import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.dto.ABasicAdminDto;
import logs.api.dto.notificationChannel.NotificationChannelDto;
import lombok.Data;

@Data
@Schema
public class NotificationGroupDto extends ABasicAdminDto {
    @Schema(name = "name")
    private String name;
    @Schema(name = "description")
    private String description;
    @Schema(name = "notificationChannel")
    private NotificationChannelDto notificationChannel;
    @Schema(name = "timeFrame")
    private Integer timeFrame;
}
