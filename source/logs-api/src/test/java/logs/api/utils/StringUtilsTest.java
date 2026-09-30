package logs.api.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @Test
    void shouldReturnLowercaseAlphanumericOfRequestedLengthWhenGeneratingRandomString() {
        String value = StringUtils.generateRandomString(32);

        assertThat(value).hasSize(32).matches("[a-z0-9]+");
    }
}
