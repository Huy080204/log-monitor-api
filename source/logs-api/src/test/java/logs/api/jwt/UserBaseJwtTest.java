package logs.api.jwt;

import logs.api.utils.ZipUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserBaseJwtTest {

    @Test
    void shouldParseAttributeDtoIgnoringUnknownFieldsWhenDecodingToken() {
        String raw = "7|app|<>|kind|perm|<>|1|admin|<>|<>|{\"isSuperAdmin\":true,\"unknownField\":1}";

        UserBaseJwt jwt = UserBaseJwt.decode(ZipUtils.zipString(raw));

        assertThat(jwt).isNotNull();
        assertThat(jwt.getAccountId()).isEqualTo(7L);
        assertThat(jwt.getAttributeDto().getIsSuperAdmin()).isTrue();
    }
}
