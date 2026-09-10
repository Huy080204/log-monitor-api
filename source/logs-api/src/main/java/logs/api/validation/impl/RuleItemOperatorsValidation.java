package logs.api.validation.impl;

import logs.api.form.notificationRule.NotificationRuleItemForm;
import logs.api.validation.RuleItemOperators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class RuleItemOperatorsValidation implements ConstraintValidator<RuleItemOperators, List<NotificationRuleItemForm>> {

    @Override
    public void initialize(RuleItemOperators constraintAnnotation) {
    }

    @Override
    public boolean isValid(List<NotificationRuleItemForm> value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        for (int i = 1; i < value.size(); i++) {
            if (value.get(i).getOperator() == null) {
                return false;
            }
        }
        return true;
    }
}
