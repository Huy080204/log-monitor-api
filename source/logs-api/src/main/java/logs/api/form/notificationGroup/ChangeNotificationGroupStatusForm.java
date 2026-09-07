package logs.api.form.notificationGroup;

import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.validation.NotificationGroupStatus;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema
public class ChangeNotificationGroupStatusForm {
    @NotificationGroupStatus
    @NotNull(message = "status cannot be null")
    @Schema(name = "status", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
