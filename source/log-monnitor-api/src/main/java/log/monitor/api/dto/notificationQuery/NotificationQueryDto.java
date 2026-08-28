package log.monitor.api.dto.notificationQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import log.monitor.api.dto.ABasicAdminDto;
import log.monitor.api.dto.notificationGroup.NotificationGroupDto;
import lombok.Data;

@Data
@Schema
public class NotificationQueryDto extends ABasicAdminDto {
    @Schema(name = "query")
    private String query;
    @Schema(name = "count")
    private Integer count;
    @Schema(name = "notificationGroup")
    private NotificationGroupDto notificationGroup;
}
