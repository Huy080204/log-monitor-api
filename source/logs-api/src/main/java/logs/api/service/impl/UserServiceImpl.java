package logs.api.service.impl;

import logs.api.jwt.UserBaseJwt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Transactional
@Service(value = "userService")
@Slf4j
public class UserServiceImpl {
    public String getCurrentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            OAuth2AuthenticationDetails oauthDetails =
                    (OAuth2AuthenticationDetails) authentication.getDetails();
            if (oauthDetails != null) {
                return oauthDetails.getTokenValue();
            }
        }
        return null;
    }

    public UserBaseJwt getAddInfoFromToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            OAuth2AuthenticationDetails oauthDetails =
                    (OAuth2AuthenticationDetails) authentication.getDetails();
            if (oauthDetails != null) {
                Map<String, Object> map = (Map<String, Object>) oauthDetails.getDecodedDetails();
                String encodedData = (String) map.get("additional_info");
                //idStr -> json
                if (encodedData != null && !encodedData.isEmpty()) {
                    return UserBaseJwt.decode(encodedData);
                }
                return null;
            }
        }
        return null;
    }
    @SuppressWarnings("unchecked")
    private Map<String, Object> getDecodedDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            OAuth2AuthenticationDetails oauthDetails = (OAuth2AuthenticationDetails) authentication.getDetails();
            if (oauthDetails != null && oauthDetails.getDecodedDetails() != null) {
                return (Map<String, Object>) oauthDetails.getDecodedDetails();
            }
        }
        return null;
    }
    @SuppressWarnings("unchecked")
    public List<String> getAuthorities() {
        Map<String, Object> decodedDetails = getDecodedDetails();
        if (decodedDetails != null) {
            List<String> authorities = (List<String>) decodedDetails.get("authorities");
            if (authorities != null) {
                return authorities.stream().map(authority -> authority.replace("ROLE_", "")).collect(Collectors.toList());
            }
        }
        return null;
    }

}
