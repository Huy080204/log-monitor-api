package logs.api.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomJwtTokenValidatorTest {
    private static final int VERSION = 1;
    private static final Long APP_ID = 9788096411467776L;
    private static final String RESOURCE_ID = "user-base-service";

    private final CustomJwtTokenValidator validator = new CustomJwtTokenValidator(VERSION, APP_ID, RESOURCE_ID);

    @Test
    void shouldSucceedWhenVersionAndAppIdMatchAndAudienceIsAbsent() {
        Jwt jwt = jwtBuilder().claim("version", VERSION).claim("app_name", APP_ID.toString()).build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void shouldSucceedWhenAudienceContainsResourceId() {
        Jwt jwt = jwtBuilder().claim("version", VERSION).claim("app_name", APP_ID.toString())
                .audience(List.of(RESOURCE_ID)).build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void shouldFailWhenVersionDiffers() {
        Jwt jwt = jwtBuilder().claim("version", VERSION + 1).claim("app_name", APP_ID.toString()).build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.getErrors()).extracting(OAuth2Error::getDescription)
                .containsExactly("Token has been force expired (version mismatch)");
    }

    @Test
    void shouldFailWhenVersionIsMissing() {
        Jwt jwt = jwtBuilder().claim("app_name", APP_ID.toString()).build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.getErrors()).extracting(OAuth2Error::getDescription)
                .containsExactly("Token has been force expired (version mismatch)");
    }

    @Test
    void shouldFailWhenAppIdDiffers() {
        Jwt jwt = jwtBuilder().claim("version", VERSION).claim("app_name", "1").build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.getErrors()).extracting(OAuth2Error::getDescription)
                .containsExactly("Token has been force expired (appId mismatch)");
    }

    @Test
    void shouldFailWhenAppIdIsNotNumeric() {
        Jwt jwt = jwtBuilder().claim("version", VERSION).claim("app_name", "logs-api").build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.getErrors()).extracting(OAuth2Error::getDescription)
                .containsExactly("Token has been force expired (appId mismatch)");
    }

    @Test
    void shouldFailWhenAppIdIsMissing() {
        Jwt jwt = jwtBuilder().claim("version", VERSION).build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.getErrors()).extracting(OAuth2Error::getDescription)
                .containsExactly("Token has been force expired (appId mismatch)");
    }

    @Test
    void shouldFailWhenAudienceDoesNotContainResourceId() {
        Jwt jwt = jwtBuilder().claim("version", VERSION).claim("app_name", APP_ID.toString())
                .audience(List.of("other-service")).build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.getErrors()).extracting(OAuth2Error::getDescription)
                .containsExactly("Invalid token does not contain resource id (user-base-service)");
    }

    private static Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("token-value").header("alg", "RS256");
    }
}
