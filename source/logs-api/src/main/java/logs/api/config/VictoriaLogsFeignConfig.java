package logs.api.config;

import feign.RequestInterceptor;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import logs.api.service.feign.codec.FormUrlEncodedEncoder;
import logs.api.service.feign.codec.NdJsonDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class VictoriaLogsFeignConfig {

    @Value("${victorialogs.api.username}")
    private String username;

    @Value("${victorialogs.api.password}")
    private String password;

    @Bean
    public RequestInterceptor victoriaLogsBasicAuthRequestInterceptor() {
        return new BasicAuthRequestInterceptor(username, password);
    }

    @Bean
    public Encoder victoriaLogsEncoder() {
        return new FormUrlEncodedEncoder();
    }

    @Bean
    public Decoder victoriaLogsDecoder() {
        return new NdJsonDecoder();
    }
}
