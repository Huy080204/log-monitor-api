package logs.api.config.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;

/**
 * Builds the Authentication the way the legacy DefaultAccessTokenConverter did:
 * authorities from the "authorities" claim with no prefix, name from "user_name",
 * falling back to "client_id" for a client-only token.
 */
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    static final String CLAIM_AUTHORITIES = "authorities";
    static final String CLAIM_USER_NAME = "user_name";
    static final String CLAIM_CLIENT_ID = "client_id";

    private final JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

    public CustomJwtAuthenticationConverter() {
        authoritiesConverter.setAuthoritiesClaimName(CLAIM_AUTHORITIES);
        authoritiesConverter.setAuthorityPrefix("");
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);
        return new JwtAuthenticationToken(jwt, authorities, resolveName(jwt));
    }

    private static String resolveName(Jwt jwt) {
        if (jwt.hasClaim(CLAIM_USER_NAME)) {
            return jwt.getClaimAsString(CLAIM_USER_NAME);
        }
        if (jwt.hasClaim(CLAIM_CLIENT_ID)) {
            return jwt.getClaimAsString(CLAIM_CLIENT_ID);
        }
        return jwt.getSubject();
    }
}
