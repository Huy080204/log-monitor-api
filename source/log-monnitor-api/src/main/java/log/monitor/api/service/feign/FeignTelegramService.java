package log.monitor.api.service.feign;

import log.monitor.api.config.CustomFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "telegramClient", url = "${telegrambots.api.url}", configuration = CustomFeignConfig.class)
public interface FeignTelegramService {
    @PostMapping("/bot{token}/sendMessage")
    String sendMessage(@PathVariable("token") String token,
                        @RequestHeader(FeignSSOService.LOGIN_TYPE) String loginType,
                        @RequestBody Map<String, Object> payload);
}
