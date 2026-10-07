package logs.api.config;

import logs.api.config.security.CustomOAuth2AuthEntryPoint;
import logs.api.config.security.CustomJwtAuthenticationConverter;
import logs.api.config.security.CustomJwtTokenValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final String RESOURCE_ID = "user-base-service";
    private static final String[] PUBLIC_PATHS = {
            "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/index", "/pub/**",
            "/actuator/health", "/actuator/health/**",
            "/v1/account/synchronize/**",
            "/v1/setting/find-by-key", "/v1/setting/find-by-group", "/v1/setting/public",
            "/v1/*/public/**"
    };

    @Autowired
    private CustomOAuth2AuthEntryPoint customOAuth2AuthEntryPoint;

    @Value("${qrauth.auth.public.key}")
    private String publicKey;

    @Value("${jwt.version}")
    private Integer currentVersion;

    @Value("${sso.app.id}")
    private Long appId;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new CustomJwtAuthenticationConverter()))
                        .authenticationEntryPoint(customOAuth2AuthEntryPoint))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customOAuth2AuthEntryPoint)
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler()));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = Base64.getDecoder().decode(publicKey.replaceAll("\\s", ""));
        RSAPublicKey rsaPublicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(keyBytes));
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(rsaPublicKey).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                new CustomJwtTokenValidator(currentVersion, appId, RESOURCE_ID)));
        return decoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("HEAD", "GET", "POST", "PUT", "OPTIONS", "DELETE", "PATCH"));
        config.setAllowedHeaders(List.of(
                "Accept", "Origin", "Content-Type", "Depth", "User-Agent", "If-Modified-Since",
                "Cache-Control", "Authorization", "X-Req", "X-File-Size", "X-Requested-With", "X-File-Name",
                "Content-Disposition", "X-Client-Type"));
        config.setExposedHeaders(List.of("Origin", "X-Requested-With", "Content-Type", "Accept", "Authorization", "Content-Disposition"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
