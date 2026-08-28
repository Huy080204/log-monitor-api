package log.monitor.api.service.feign;

import log.monitor.api.service.impl.UserServiceImpl;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
@Component
public class OAuth2FeignRequestInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_TOKEN_TYPE = "Bearer";
    private static final String BASIC_AUTH_TYPE = "Basic";

    @Value("${sso.basic.username}")
    private String internalAuthUsername;
    @Value("${sso.basic.password}")
    private String internalAuthPassword;
    @Autowired
    private UserServiceImpl userService;

    @Override
    public void apply(RequestTemplate template) {
        if (template.headers().containsKey(FeignSSOService.LOGIN_TYPE)) {
            Object loginType = template.headers().get(FeignSSOService.LOGIN_TYPE).toArray()[0];
            if (Objects.equals(loginType, FeignConst.LOGIN_TYPE_INTERNAL)) {
                String auth = internalAuthUsername + ":" + internalAuthPassword;
                log.error("-----------> internal = " + auth);
                byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
                template.header(AUTHORIZATION_HEADER, String.format("%s %s", BASIC_AUTH_TYPE, new String(encodedAuth)));
            } else if (Objects.equals(loginType, FeignConst.LOGIN_TYPE_NO_AUTH)) {
                // external/third-party API
            } else {
                log.error("-----------> not found type = " + loginType);
            }
            template.removeHeader(FeignSSOService.LOGIN_TYPE);
        } else {
            if (userService.getCurrentToken() != null) {
                log.error("-----------> Constructing Header {} for Token {}, token {}", AUTHORIZATION_HEADER, BEARER_TOKEN_TYPE, String.format("%s %s", BEARER_TOKEN_TYPE, userService.getCurrentToken()));
                template.header(AUTHORIZATION_HEADER, String.format("%s %s", BEARER_TOKEN_TYPE, userService.getCurrentToken()));
            }
        }
    }
}
