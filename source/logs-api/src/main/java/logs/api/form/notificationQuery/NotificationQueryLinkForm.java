package logs.api.form.notificationQuery;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.form.StringToLongDeserializer;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema
public class NotificationQueryLinkForm {
    @NotNull(message = "queryTemplateId cannot be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "queryTemplateId", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long queryTemplateId;

    @NotNull(message = "applicationId cannot be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "applicationId", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long applicationId;
}
