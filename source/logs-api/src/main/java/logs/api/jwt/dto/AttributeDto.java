package logs.api.jwt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttributeDto {
    @Schema(name = "isSuperAdmin")
    private Boolean isSuperAdmin = false;
}
