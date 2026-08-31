package logs.api.config.security;

import logs.api.exception.ForceTokenExpired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import java.util.Map;
import java.util.Objects;

@Slf4j
public class CustomJwtAccessTokenConverter extends JwtAccessTokenConverter {
    private int currentVersion;
    private Long currentAppId;

    public void setCurrentVersion(int currentVersion) {
        this.currentVersion = currentVersion;
    }

    public void setCurrentAppId(Long currentAppId) {
        this.currentAppId = currentAppId;
    }

    @Override
    public OAuth2Authentication extractAuthentication(Map<String, ?> claims) {

        Integer tokenVersion = (Integer) claims.get("version");
        Long tokenAppId = null;
        try {
            tokenAppId = Long.parseLong((String) claims.get("app_name"));
        } catch (Exception e) {
            log.error("Get token app id failed", e);
        }
        //log.debug("================> vao check token tokenVersion: " + tokenVersion);
        if (tokenVersion == null || tokenVersion != currentVersion) {
            throw new ForceTokenExpired("Token has been force expired (version mismatch)");
        }
        if (tokenAppId == null || !Objects.equals(tokenAppId, currentAppId)) {
            throw new ForceTokenExpired("Token has been force expired (appId mismatch)");
        }
        // Nếu hợp lệ thì gọi logic gốc
        return super.extractAuthentication(claims);
    }
}
