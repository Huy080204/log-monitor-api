package log.monitor.api.validation.impl;

import log.monitor.api.constant.BaseConstant;
import log.monitor.api.validation.NotificationChannelType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class NotificationChannelTypeValidation implements ConstraintValidator<NotificationChannelType, Integer> {

    private boolean allowNull;

    @Override
    public void initialize(NotificationChannelType constraintAnnotation) {
        allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(Integer type, ConstraintValidatorContext constraintValidatorContext) {
        if (type == null) {
            return allowNull;
        }
        return BaseConstant.NOTIFICATION_CHANNEL_TYPE_TELEGRAM.equals(type)
                || BaseConstant.NOTIFICATION_CHANNEL_TYPE_SLACK.equals(type);
    }
}
