package logs.api.dto.queryTemplate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import logs.api.dto.ABasicAdminDto;
import logs.api.dto.applications.ApplicationsDto;

@Data
@Schema
public class QueryTemplateDto extends ABasicAdminDto {
    @Schema(name = "name")
    private String name;

    @Schema(name = "query")
    private String query;

    @Schema(name = "count")
    private Integer count;

    @Schema(name = "application")
    private ApplicationsDto application;
}
