package logs.api.form.notificationChannel;

import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.validation.NotificationChannelType;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@Schema
public class CreateNotificationChannelForm {
    @NotBlank(message = "name cannot be null")
    @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "description cannot be null")
    @Schema(name = "description", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @NotBlank(message = "channelSetting cannot be null")
    @Schema(name = "channelSetting", requiredMode = Schema.RequiredMode.REQUIRED)
    private String channelSetting;

    @NotificationChannelType
    @Schema(name = "type", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer type;
}
