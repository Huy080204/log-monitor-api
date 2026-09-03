package logs.api.form.notificationQuery;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.form.StringToLongDeserializer;
import logs.api.validation.NotificationQueryStatus;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema
public class UpdateNotificationQueryForm {
    @NotNull(message = "id cannot be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "queryTemplateId cannot be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "queryTemplateId", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long queryTemplateId;

    @NotNull(message = "status cannot be null")
    @NotificationQueryStatus
    @Schema(name = "status", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
