package logs.api.config;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local", "dev", "staging"})
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi logsApi() {
        return GroupedOpenApi.builder()
                .group("logs-api")
                .packagesToScan("logs.api.controller")
                .build();
    }
}
