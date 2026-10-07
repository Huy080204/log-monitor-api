package logs.api.service.impl;

import logs.api.jwt.UserBaseJwt;
import logs.api.utils.ZipUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceImplTest {

    private final UserServiceImpl userService = new UserServiceImpl();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnTokenValueWhenAuthenticatedWithJwt() {
        authenticate(jwtBuilder().claim("user_name", "admin").build());

        String token = userService.getCurrentToken();

        assertThat(token).isEqualTo("token-value");
    }

    @Test
    void shouldReturnNullTokenWhenAnonymous() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        String token = userService.getCurrentToken();

        assertThat(token).isNull();
    }

    @Test
    void shouldDecodeAdditionalInfoWhenClaimPresent() {
        String additionalInfo = ZipUtils.zipString("42|logs-api|1|kind|perm|1|2|admin|1|1|{}|tenant-1");
        authenticate(jwtBuilder().claim("additional_info", additionalInfo).build());

        UserBaseJwt userBaseJwt = userService.getAddInfoFromToken();

        assertThat(userBaseJwt.getAccountId()).isEqualTo(42L);
        assertThat(userBaseJwt.getUsername()).isEqualTo("admin");
        assertThat(userBaseJwt.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void shouldReturnNullAddInfoWhenClaimMissing() {
        authenticate(jwtBuilder().claim("user_name", "admin").build());

        UserBaseJwt userBaseJwt = userService.getAddInfoFromToken();

        assertThat(userBaseJwt).isNull();
    }

    @Test
    void shouldReturnNullAddInfoWhenNotAuthenticated() {
        UserBaseJwt userBaseJwt = userService.getAddInfoFromToken();

        assertThat(userBaseJwt).isNull();
    }

    @Test
    void shouldStripRolePrefixFromAuthorities() {
        authenticate(jwtBuilder().claim("authorities", List.of("ROLE_ADMIN", "ACC_V")).build());

        List<String> authorities = userService.getAuthorities();

        assertThat(authorities).containsExactly("ADMIN", "ACC_V");
    }

    @Test
    void shouldReturnNullAuthoritiesWhenClaimMissing() {
        authenticate(jwtBuilder().claim("user_name", "admin").build());

        List<String> authorities = userService.getAuthorities();

        assertThat(authorities).isNull();
    }

    @Test
    void shouldReturnNullAuthoritiesWhenNotAuthenticated() {
        List<String> authorities = userService.getAuthorities();

        assertThat(authorities).isNull();
    }

    private static void authenticate(Jwt jwt) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private static Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("token-value").header("alg", "RS256");
    }
}
