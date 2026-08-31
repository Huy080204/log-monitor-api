package logs.api.config;

import feign.RequestInterceptor;
import feign.auth.BasicAuthRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VictoriaLogsFeignConfig {

    @Value("${victorialogs.api.username}")
    private String username;

    @Value("${victorialogs.api.password}")
    private String password;

    @Bean
    public RequestInterceptor victoriaLogsBasicAuthRequestInterceptor() {
        return new BasicAuthRequestInterceptor(username, password);
    }
}
