package logs.api.service.feign;

import logs.api.config.CustomFeignConfig;
import logs.api.config.VictoriaLogsFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@FeignClient(name = "victoria-logs-svr", url = "${victorialogs.api.url}", configuration = {CustomFeignConfig.class, VictoriaLogsFeignConfig.class})
public interface FeignVictoriaLogsService {

    @PostMapping(value = "/select/logsql/query", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String queryForm(@RequestHeader(FeignSSOService.LOGIN_TYPE) String loginType, @RequestBody String formBody);

    default String query(String loginType, String query) {
        return queryForm(loginType, "query=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
    }
}
