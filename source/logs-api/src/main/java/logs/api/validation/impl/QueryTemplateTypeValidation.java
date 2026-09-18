package logs.api.validation.impl;

import logs.api.constant.BaseConstant;
import logs.api.validation.QueryTemplateType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class QueryTemplateTypeValidation implements ConstraintValidator<QueryTemplateType, Integer> {

    private boolean allowNull;

    @Override
    public void initialize(QueryTemplateType constraintAnnotation) {
        allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(Integer queryTemplateType, ConstraintValidatorContext constraintValidatorContext) {
        if (queryTemplateType == null) {
            return allowNull;
        }
        return BaseConstant.QUERY_TEMPLATE_TYPE_THRESHOLD.equals(queryTemplateType)
                || BaseConstant.QUERY_TEMPLATE_TYPE_CUSTOM.equals(queryTemplateType);
    }
}
