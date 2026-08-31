package logs.api.config;

import logs.api.config.security.CustomJwtAccessTokenConverter;
import logs.api.config.security.CustomOAuth2AuthEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
    @Autowired
    JsonToUrlEncodedAuthenticationFilter jsonFilter;

    @Autowired
    private CustomOAuth2AuthEntryPoint customOAuth2AuthEntryPoint;

    @Value("${qrauth.auth.public.key}")
    private String publicKey;

    @Value("${jwt.version}")
    private Integer currentVersion;

    @Value("${sso.app.id}")
    private Long appId;

    @Bean
    public JwtAccessTokenConverter accessTokenConverter() throws NoSuchAlgorithmException, InvalidKeySpecException {
        CustomJwtAccessTokenConverter converter = new CustomJwtAccessTokenConverter();
        converter.setCurrentVersion(currentVersion);
        converter.setCurrentAppId(appId);
        converter.setAccessTokenConverter(new CustomTokenConverter());
        String publicKeyPEM = publicKey.replaceAll("\\s", "");
        String fullPublicKey = "-----BEGIN PUBLIC KEY-----\n" + publicKeyPEM + "\n-----END PUBLIC KEY-----";
        converter.setVerifierKey(fullPublicKey);
        return converter;
    }

    @Bean
    public DefaultTokenServices createTokenServices() throws NoSuchAlgorithmException, InvalidKeySpecException {
        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore(new JwtTokenStore(accessTokenConverter()));
        return defaultTokenServices;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.addFilterAfter(jsonFilter, BasicAuthenticationFilter.class)
                .requestMatchers()
                .and()
                .authorizeRequests()
                .antMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/index", "/pub/**", "/api/token", "/api/auth/pwd/verify-token",
                        "/api/auth/activate/resend", "/api/auth/pwd", "/api/auth/logout", "/actuator/**").permitAll()
                .antMatchers("/v1/account/synchronize/**").permitAll()
                .antMatchers("/v1/setting/find-by-key", "/v1/setting/find-by-group", "/v1/setting/public").permitAll()
                .antMatchers("/v1/*/public/**").permitAll()
                .antMatchers("/**").authenticated()
                .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().exceptionHandling()
                .accessDeniedHandler(new OAuth2AccessDeniedHandler())
                .authenticationEntryPoint(customOAuth2AuthEntryPoint);
        ;
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        resources
                .resourceId("user-base-service")
                .authenticationEntryPoint(customOAuth2AuthEntryPoint);
    }
}