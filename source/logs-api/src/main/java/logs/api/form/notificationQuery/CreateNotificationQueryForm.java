package logs.api.form.notificationQuery;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.form.StringToLongDeserializer;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@Schema
public class CreateNotificationQueryForm {
    @NotNull(message = "notificationGroupId cannot be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "notificationGroupId", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long notificationGroupId;

    @NotNull(message = "links cannot be null")
    @Valid
    @Schema(name = "links", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<NotificationQueryLinkForm> links;
}
