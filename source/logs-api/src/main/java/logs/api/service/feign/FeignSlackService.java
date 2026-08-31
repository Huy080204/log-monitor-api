package logs.api.service.feign;

import logs.api.config.CustomFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "slackClient", url = "${slack.api.url}", configuration = CustomFeignConfig.class)
public interface FeignSlackService {
    @PostMapping(value = "/chat.postMessage", consumes = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> postMessage(@RequestHeader("Authorization") String bearerToken,
                                     @RequestHeader(FeignSSOService.LOGIN_TYPE) String loginType,
                                     @RequestBody Map<String, Object> payload);
}
