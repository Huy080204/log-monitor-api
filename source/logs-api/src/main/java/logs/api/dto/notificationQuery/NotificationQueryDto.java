package logs.api.dto.notificationQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.dto.ABasicAdminDto;
import logs.api.dto.applications.ApplicationsDto;
import logs.api.dto.notificationGroup.NotificationGroupDto;
import logs.api.dto.queryTemplate.QueryTemplateDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema
public class NotificationQueryDto extends ABasicAdminDto {
    @Schema(name = "notificationGroup")
    private NotificationGroupDto notificationGroup;
    @Schema(name = "queryTemplate")
    private QueryTemplateDto queryTemplate;
    @Schema(name = "application")
    private ApplicationsDto application;
}
