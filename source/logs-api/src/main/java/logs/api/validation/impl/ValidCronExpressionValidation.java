package logs.api.validation.impl;

import logs.api.validation.ValidCronExpression;
import org.quartz.CronExpression;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ValidCronExpressionValidation implements ConstraintValidator<ValidCronExpression, String> {

    private boolean allowNull;

    @Override
    public void initialize(ValidCronExpression constraintAnnotation) {
        allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(String cronExpression, ConstraintValidatorContext constraintValidatorContext) {
        if (cronExpression == null) {
            return allowNull;
        }
        if (cronExpression.trim().isEmpty()) {
            return false;
        }
        return CronExpression.isValidExpression(cronExpression);
    }
}
