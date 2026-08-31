package logs.api.jwt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Data
@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttributeDto {
    @Schema(name = "isSuperAdmin")
    private Boolean isSuperAdmin = false;
}
