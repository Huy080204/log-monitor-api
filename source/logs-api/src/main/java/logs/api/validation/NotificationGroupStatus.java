package logs.api.validation;

import logs.api.validation.impl.NotificationGroupStatusValidation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotificationGroupStatusValidation.class)
@Documented
public @interface NotificationGroupStatus {
    boolean allowNull() default false;
    String message() default "Notification group status invalid.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
