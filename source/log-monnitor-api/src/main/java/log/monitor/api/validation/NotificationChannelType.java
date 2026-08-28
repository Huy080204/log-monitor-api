package log.monitor.api.validation;

import log.monitor.api.validation.impl.NotificationChannelTypeValidation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotificationChannelTypeValidation.class)
@Documented
public @interface NotificationChannelType {
    boolean allowNull() default false;

    String message() default "Notification channel type invalid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
