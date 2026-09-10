package logs.api.validation.impl;

import logs.api.form.notificationRule.NotificationRuleItemForm;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleItemOperatorsValidationTest {

    private final RuleItemOperatorsValidation validator = new RuleItemOperatorsValidation();

    private NotificationRuleItemForm itemWithOperator(Integer operator) {
        NotificationRuleItemForm item = new NotificationRuleItemForm();
        item.setOperator(operator);
        return item;
    }

    @Test
    void shouldBeValidWhenItemsIsNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void shouldBeValidWhenIndexZeroOperatorNeverChecked() {
        List<NotificationRuleItemForm> items = Arrays.asList(itemWithOperator(null), itemWithOperator(5));

        assertThat(validator.isValid(items, null)).isTrue();
    }

    @Test
    void shouldBeInvalidWhenIndexOneOperatorIsNull() {
        List<NotificationRuleItemForm> items = Arrays.asList(itemWithOperator(5), itemWithOperator(null));

        assertThat(validator.isValid(items, null)).isFalse();
    }
}
