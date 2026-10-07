package logs.api.config.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Checks the token claims the legacy CustomJwtAccessTokenConverter and
 * ResourceServerSecurityConfigurer.resourceId(...) used to check: token version, owning app and audience.
 */
public class CustomJwtTokenValidator implements OAuth2TokenValidator<Jwt> {
    static final String CLAIM_VERSION = "version";
    static final String CLAIM_APP_NAME = "app_name";

    private final int currentVersion;
    private final Long currentAppId;
    private final String resourceId;

    public CustomJwtTokenValidator(int currentVersion, Long currentAppId, String resourceId) {
        this.currentVersion = currentVersion;
        this.currentAppId = currentAppId;
        this.resourceId = resourceId;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Object tokenVersion = jwt.getClaims().get(CLAIM_VERSION);
        if (!(tokenVersion instanceof Number) || ((Number) tokenVersion).intValue() != currentVersion) {
            return failure("Token has been force expired (version mismatch)");
        }
        if (!currentAppId.equals(parseAppId(jwt.getClaimAsString(CLAIM_APP_NAME)))) {
            return failure("Token has been force expired (appId mismatch)");
        }
        // same rule as the legacy OAuth2AuthenticationManager: an absent/empty aud is accepted
        List<String> audience = jwt.getAudience();
        if (audience != null && !audience.isEmpty() && !audience.contains(resourceId)) {
            return failure("Invalid token does not contain resource id (" + resourceId + ")");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private static Long parseAppId(String appName) {
        if (appName == null) {
            return null;
        }
        try {
            return Long.parseLong(appName);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, description, null));
    }
}
