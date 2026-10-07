package logs.api.validation;

import logs.api.validation.impl.NotificationQueryStatusValidation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotificationQueryStatusValidation.class)
@Documented
public @interface NotificationQueryStatus {
    boolean allowNull() default false;

    String message() default "Notification query status invalid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
