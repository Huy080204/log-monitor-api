package log.monitor.api.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class AccountAppsDto {
    @NotNull(message = "accountApps cannot be null")
    @Schema(name = "accountApps")
    private List<SSOAccountDto> accountApps;

    @NotBlank(message = "attribute cannot be null")
    @Schema(name = "attribute")
    private String attribute;

    @NotBlank(message = "secretIdApp cannot be null")
    @Schema(name = "secretIdApp")
    private String secretIdApp;
}
