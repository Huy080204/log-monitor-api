package logs.api.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SSOAccountDto {
    @Schema(name = "id")
    private Long id;

    @Schema(name = "username")
    private String username;

    @Schema(name = "phone")
    private String phone;

    @Schema(name = "email")
    private String email;

    @Schema(name = "fullName")
    private String fullName;

    @Schema(name = "avatarPath")
    private String avatarPath;

    @Schema(name = "status")
    private Integer status;

    @Schema(name = "isMfa")
    private Boolean isMfa;

    @Schema(name = "attribute")
    private String attribute;

    @Schema(name = "kind")
    private Integer kind;
}
