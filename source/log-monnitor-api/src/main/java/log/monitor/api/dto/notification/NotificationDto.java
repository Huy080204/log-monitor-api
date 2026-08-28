package log.monitor.api.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import log.monitor.api.dto.ABasicAdminDto;
import log.monitor.api.dto.notificationGroup.NotificationGroupDto;
import lombok.Data;

@Data
@Schema
public class NotificationDto extends ABasicAdminDto {
    @Schema(name = "message")
    private String message;
    @Schema(name = "state")
    private Integer state;
    @Schema(name = "notificationGroup")
    private NotificationGroupDto notificationGroup;
}
