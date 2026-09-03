package logs.api.form.queryTemplate;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import logs.api.form.StringToLongDeserializer;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema
public class CreateQueryTemplateForm {
    @NotBlank(message = "name cannot be null")
    @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "query cannot be null")
    @Schema(name = "query", requiredMode = Schema.RequiredMode.REQUIRED)
    private String query;

    @NotNull(message = "count cannot be null")
    @Schema(name = "count", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer count;

    @Schema(name = "applicationId")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    private Long applicationId;
}
