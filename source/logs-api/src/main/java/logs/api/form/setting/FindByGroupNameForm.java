package logs.api.form.setting;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

@Data
@Schema
public class FindByGroupNameForm {
    @Schema(name = "groupNames")
    private String[] groupNames;

    public List<String> getGroupNames() {
        return Arrays.asList(groupNames);
    }
}
