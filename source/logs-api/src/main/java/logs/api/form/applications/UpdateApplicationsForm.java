package logs.api.form.applications;

import logs.api.form.StringToLongDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema
public class UpdateApplicationsForm {
    @NotNull(message = "id cannot be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "name cannot be null")
    @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "victoriaAppId cannot be null")
    @Schema(name = "victoriaAppId", requiredMode = Schema.RequiredMode.REQUIRED)
    private String victoriaAppId;

    @NotBlank(message = "description cannot be null")
    @Schema(name = "description", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;
}
