package logs.api.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.dto.ABasicAdminDto;
import logs.api.dto.notificationGroup.NotificationGroupDto;
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
