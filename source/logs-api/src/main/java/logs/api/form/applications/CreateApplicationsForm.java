package logs.api.form.applications;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@Schema
public class CreateApplicationsForm {
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
