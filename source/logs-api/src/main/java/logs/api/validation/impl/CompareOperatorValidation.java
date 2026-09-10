package logs.api.validation.impl;

import logs.api.constant.BaseConstant;
import logs.api.validation.CompareOperator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CompareOperatorValidation implements ConstraintValidator<CompareOperator, Integer> {

    private boolean allowNull;

    @Override
    public void initialize(CompareOperator constraintAnnotation) {
        allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(Integer operator, ConstraintValidatorContext constraintValidatorContext) {
        if (operator == null) {
            return allowNull;
        }
        return BaseConstant.RULE_ITEM_OPERATOR_EQ.equals(operator)
                || BaseConstant.RULE_ITEM_OPERATOR_NEQ.equals(operator)
                || BaseConstant.RULE_ITEM_OPERATOR_GT.equals(operator)
                || BaseConstant.RULE_ITEM_OPERATOR_GTE.equals(operator)
                || BaseConstant.RULE_ITEM_OPERATOR_LT.equals(operator)
                || BaseConstant.RULE_ITEM_OPERATOR_LTE.equals(operator);
    }
}
