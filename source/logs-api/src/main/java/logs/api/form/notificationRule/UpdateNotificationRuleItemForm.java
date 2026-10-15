package logs.api.form.notificationRule;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import logs.api.form.StringToLongDeserializer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema
public class UpdateNotificationRuleItemForm extends NotificationRuleItemForm {
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "id")
    private Long id;
}
