package logs.api.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomJwtAuthenticationConverterTest {

    private final CustomJwtAuthenticationConverter converter = new CustomJwtAuthenticationConverter();

    @Test
    void shouldUseUserNameAndUnprefixedAuthoritiesWhenTokenHasUser() {
        Jwt jwt = jwtBuilder()
                .claim("user_name", "admin")
                .claim("client_id", "abc_client")
                .claim("authorities", List.of("ACC_V", "ROLE_ADMIN"))
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication.getName()).isEqualTo("admin");
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ACC_V", "ROLE_ADMIN");
    }

    @Test
    void shouldFallBackToClientIdWhenTokenHasNoUserName() {
        Jwt jwt = jwtBuilder().claim("client_id", "abc_client").build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication.getName()).isEqualTo("abc_client");
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    void shouldFallBackToSubjectWhenTokenHasNoUserNameAndNoClientId() {
        Jwt jwt = jwtBuilder().subject("sub-user").build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication.getName()).isEqualTo("sub-user");
    }

    private static Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("token-value").header("alg", "RS256");
    }
}
