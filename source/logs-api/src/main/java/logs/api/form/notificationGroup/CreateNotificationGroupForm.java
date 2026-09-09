package logs.api.form.notificationGroup;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.form.StringToLongDeserializer;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema
public class CreateNotificationGroupForm {
    @NotBlank(message = "name cannot be null")
    @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "description cannot be null")
    @Schema(name = "description", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @NotNull(message = "notificationChannelId cannot be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "notificationChannelId", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long notificationChannelId;

    @NotNull(message = "timeFrame cannot be null")
    @Min(value = 1, message = "timeFrame must be at least 1")
    @Schema(name = "timeFrame", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer timeFrame;
}
