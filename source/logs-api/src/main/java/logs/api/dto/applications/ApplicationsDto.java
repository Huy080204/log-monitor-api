package logs.api.dto.applications;

import logs.api.dto.ABasicAdminDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema
public class ApplicationsDto extends ABasicAdminDto {
    @Schema(name = "name")
    private String name;

    @Schema(name = "victoriaAppId")
    private String victoriaAppId;

    @Schema(name = "description")
    private String description;
}
