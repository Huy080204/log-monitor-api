package logs.api.dto.account;

import logs.api.dto.ABasicAdminDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class AccountDto extends ABasicAdminDto {
    @Schema(name = "kind")
    private Integer kind;

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

    @Schema(name = "attribute")
    private String attribute;

    @Schema(name = "isSuperAdmin")
    private Boolean isSuperAdmin;

    @Schema(name = "permissions")
    private List<String> permissions;
}
