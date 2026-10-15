package logs.api.form.notificationGroup;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.form.StringToLongDeserializer;
import logs.api.validation.NotificationGroupStatus;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Schema
public class ChangeNotificationGroupStatusForm {
    @NotNull(message = "id cannot be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotificationGroupStatus
    @Schema(name = "status", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
