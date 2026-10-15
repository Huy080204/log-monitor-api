package logs.api.jwt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttributeDto {
    @Schema(name = "isSuperAdmin")
    private Boolean isSuperAdmin = false;
}
