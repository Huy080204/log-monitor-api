package logs.api.validation;

import logs.api.validation.impl.CompareOperatorValidation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CompareOperatorValidation.class)
@Documented
public @interface CompareOperator {
    boolean allowNull() default false;
    String message() default "Compare operator invalid.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
