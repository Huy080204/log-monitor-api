package logs.api.dto.notificationQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.dto.ABasicAdminDto;
import logs.api.dto.notificationGroup.NotificationGroupDto;
import logs.api.dto.queryTemplate.QueryTemplateDto;
import lombok.Data;

@Data
@Schema
public class NotificationQueryDto extends ABasicAdminDto {
    @Schema(name = "notificationGroup")
    private NotificationGroupDto notificationGroup;
    @Schema(name = "queryTemplate")
    private QueryTemplateDto queryTemplate;
}
