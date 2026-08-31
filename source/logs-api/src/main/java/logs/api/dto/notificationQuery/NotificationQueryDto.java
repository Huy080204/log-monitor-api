package logs.api.dto.notificationQuery;

import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.dto.ABasicAdminDto;
import logs.api.dto.notificationGroup.NotificationGroupDto;
import lombok.Data;

@Data
@Schema
public class NotificationQueryDto extends ABasicAdminDto {
    @Schema(name = "name")
    private String name;
    @Schema(name = "query")
    private String query;
    @Schema(name = "count")
    private Integer count;
    @Schema(name = "notificationGroup")
    private NotificationGroupDto notificationGroup;
}
