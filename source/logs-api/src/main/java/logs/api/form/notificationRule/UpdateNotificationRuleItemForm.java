package logs.api.form.notificationRule;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.form.StringToLongDeserializer;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema
public class UpdateNotificationRuleItemForm {
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "id")
    private Long id;

    @NotNull(message = "applicationId cannot be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "applicationId", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long applicationId;

    @NotNull(message = "queryTemplateId cannot be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "queryTemplateId", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long queryTemplateId;

    @Schema(name = "operator")
    private Integer operator;
}
