package logs.api.form.notificationRule;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.form.StringToLongDeserializer;
import logs.api.validation.RuleItemOperators;
import lombok.Data;

import logs.api.constant.BaseConstant;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@Schema
public class UpdateNotificationRuleForm {
    @NotNull(message = "id cannot be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "name cannot be null")
    @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(name = "description")
    private String description;

    @Valid
    @NotNull(message = "items cannot be null")
    @Size(min = 2, message = "items must have at least 2 elements")
    @RuleItemOperators
    @Schema(name = "items", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<@Valid UpdateNotificationRuleItemForm> items;
}
