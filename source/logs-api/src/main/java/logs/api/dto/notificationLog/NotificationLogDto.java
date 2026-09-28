package logs.api.dto.notificationLog;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.dto.ABasicAdminDto;
import logs.api.dto.LongToStringIfWebSerializer;
import lombok.Data;

@Data
@Schema
public class NotificationLogDto extends ABasicAdminDto {
    @Schema(name = "appId")
    @JsonSerialize(using = LongToStringIfWebSerializer.class)
    private Long appId;
    @Schema(name = "appName")
    private String appName;
    @Schema(name = "errorName")
    private String errorName;
    @Schema(name = "link")
    private String link;
}
