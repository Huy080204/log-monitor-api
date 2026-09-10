package logs.api.dto.notificationRule;

import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.dto.ABasicAdminDto;
import logs.api.dto.applications.ApplicationsDto;
import logs.api.dto.queryTemplate.QueryTemplateDto;
import lombok.Data;

@Data
@Schema
public class NotificationRuleItemDto extends ABasicAdminDto {
    @Schema(name = "application")
    private ApplicationsDto application;
    @Schema(name = "queryTemplate")
    private QueryTemplateDto queryTemplate;
    @Schema(name = "ordering")
    private Integer ordering;
    @Schema(name = "operator")
    private Integer operator;
}
