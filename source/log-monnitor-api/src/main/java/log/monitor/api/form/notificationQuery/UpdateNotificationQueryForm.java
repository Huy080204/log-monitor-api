package log.monitor.api.form.notificationQuery;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import log.monitor.api.form.StringToLongDeserializer;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema
public class UpdateNotificationQueryForm {
    @NotNull(message = "id cannot be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "query cannot be null")
    @Schema(name = "query", requiredMode = Schema.RequiredMode.REQUIRED)
    private String query;

    @NotNull(message = "count cannot be null")
    @Schema(name = "count", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer count;
}
