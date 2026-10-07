package logs.api.service.impl;

import logs.api.jwt.UserBaseJwt;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service(value = "userService")
public class UserServiceImpl {
    public String getCurrentToken() {
        Jwt jwt = getCurrentJwt();
        return jwt != null ? jwt.getTokenValue() : null;
    }

    public UserBaseJwt getAddInfoFromToken() {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return null;
        }
        //idStr -> json
        String encodedData = jwt.getClaimAsString("additional_info");
        if (encodedData != null && !encodedData.isEmpty()) {
            return UserBaseJwt.decode(encodedData);
        }
        return null;
    }

    public List<String> getAuthorities() {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return null;
        }
        List<String> authorities = jwt.getClaimAsStringList("authorities");
        if (authorities != null) {
            return authorities.stream().map(authority -> authority.replace("ROLE_", "")).collect(Collectors.toList());
        }
        return null;
    }

    private Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            return ((JwtAuthenticationToken) authentication).getToken();
        }
        return null;
    }
}
