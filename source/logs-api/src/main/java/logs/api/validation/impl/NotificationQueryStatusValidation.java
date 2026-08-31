package logs.api.validation.impl;

import logs.api.constant.BaseConstant;
import logs.api.validation.NotificationQueryStatus;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class NotificationQueryStatusValidation implements ConstraintValidator<NotificationQueryStatus, Integer> {

    private boolean allowNull;

    @Override
    public void initialize(NotificationQueryStatus constraintAnnotation) {
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
