package log.monitor.api.form.setting;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

@Data
@Schema
public class FindByKeyNameForm {
    @Schema(name = "keyNames")
    private String[] keyNames;

    public List<String> getKeyNames() {
        return Arrays.asList(keyNames);
    }
}
