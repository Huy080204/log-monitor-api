package logs.api.form.notificationGroup;

import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.validation.NotificationGroupStatus;
import lombok.Data;

@Data
@Schema
public class ChangeNotificationGroupStatusForm {
    @NotificationGroupStatus
    @Schema(name = "status", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
