package logs.api.validation.impl;

import logs.api.constant.BaseConstant;
import logs.api.validation.NotificationGroupStatus;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class NotificationGroupStatusValidation implements ConstraintValidator<NotificationGroupStatus, Integer> {

    private boolean allowNull;

    @Override
    public void initialize(NotificationGroupStatus constraintAnnotation) {
        allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(Integer status, ConstraintValidatorContext constraintValidatorContext) {
        if (status == null) {
            return allowNull;
        }
        return BaseConstant.STATUS_ACTIVE.equals(status)
                || BaseConstant.STATUS_PENDING.equals(status);
    }
}
