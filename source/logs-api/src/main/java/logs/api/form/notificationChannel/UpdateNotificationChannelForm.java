package logs.api.form.notificationChannel;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.form.StringToLongDeserializer;
import logs.api.validation.NotificationChannelType;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema
public class UpdateNotificationChannelForm {
    @NotNull(message = "id cannot be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

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
