package logs.api.validation.impl;

import logs.api.constant.BaseConstant;
import logs.api.validation.NotificationGroupCheckType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class NotificationGroupCheckTypeValidation implements ConstraintValidator<NotificationGroupCheckType, Integer> {

    private boolean allowNull;

    @Override
    public void initialize(NotificationGroupCheckType constraintAnnotation) {
        allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(Integer checkType, ConstraintValidatorContext constraintValidatorContext) {
        if (checkType == null) {
            return allowNull;
        }
        return BaseConstant.NOTIFICATION_GROUP_CHECK_TYPE_THRESHOLD.equals(checkType)
                || BaseConstant.NOTIFICATION_GROUP_CHECK_TYPE_COMPARISON.equals(checkType);
    }
}
