package logs.api.form.notificationQuery;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.form.StringToLongDeserializer;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Schema
public class ReplaceNotificationQueryForm {
    @NotNull(message = "notificationGroupId cannot be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "notificationGroupId", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long notificationGroupId;

    @NotEmpty(message = "templateQueryIds cannot be empty")
    @JsonDeserialize(contentUsing = StringToLongDeserializer.class)
    @Schema(name = "templateQueryIds", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> templateQueryIds;
}
