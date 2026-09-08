package logs.api.form.notificationGroup;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.form.StringToLongDeserializer;
import logs.api.validation.NotificationChannelType;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema
public class UpdateNotificationGroupForm {
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

    @Schema(name = "channelSetting")
    private String channelSetting;

    @NotificationChannelType(allowNull = true)
    @Schema(name = "type")
    private Integer type;

    @NotNull(message = "timeFrame cannot be null")
    @Min(value = 1, message = "timeFrame must be at least 1")
    @Schema(name = "timeFrame", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer timeFrame;
}
