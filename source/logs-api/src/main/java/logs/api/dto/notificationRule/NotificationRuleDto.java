package logs.api.dto.notificationRule;

import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.dto.ABasicAdminDto;
import logs.api.dto.notificationGroup.NotificationGroupDto;
import lombok.Data;

@Data
@Schema
public class NotificationRuleDto extends ABasicAdminDto {
    @Schema(name = "name")
    private String name;
    @Schema(name = "description")
    private String description;
    @Schema(name = "notificationGroup")
    private NotificationGroupDto notificationGroup;
}
