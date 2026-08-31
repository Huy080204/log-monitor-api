package logs.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ABasicAdminDto {
    @Schema(name = "id")
    @JsonSerialize(using = LongToStringIfWebSerializer.class)
    private Long id;

    @Schema(name = "status")
    private Integer status;

    @Schema(name = "modifiedDate")
    private LocalDateTime modifiedDate;

    @Schema(name = "createdDate")
    private LocalDateTime createdDate;
}
