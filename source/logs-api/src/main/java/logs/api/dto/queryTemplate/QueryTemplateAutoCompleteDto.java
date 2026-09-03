package logs.api.dto.queryTemplate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import logs.api.dto.ABasicAdminDto;

@Data
@Schema
public class QueryTemplateAutoCompleteDto extends ABasicAdminDto {
    @Schema(name = "name")
    private String name;
}
