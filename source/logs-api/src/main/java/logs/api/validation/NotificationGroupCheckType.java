package logs.api.validation;

import logs.api.validation.impl.NotificationGroupCheckTypeValidation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotificationGroupCheckTypeValidation.class)
@Documented
public @interface NotificationGroupCheckType {
    boolean allowNull() default false;
    String message() default "NotificationGroup checkType invalid.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
