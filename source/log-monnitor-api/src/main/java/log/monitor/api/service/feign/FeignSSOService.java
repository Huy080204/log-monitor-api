package log.monitor.api.service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import log.monitor.api.config.CustomFeignConfig;
import log.monitor.api.dto.ApiMessageDto;

@FeignClient(name = "sso-svr", url = "${sso.internal.base.url}", configuration = CustomFeignConfig.class)
public interface FeignSSOService {
    String LOGIN_TYPE = "BASIC_LOGIN_AUTH";
    String HEADER_AUTHORIZATION = "Authorization";

    @GetMapping(value = "/v1/account/verify-token")
    ApiMessageDto<String> verifyToken(@RequestHeader(HEADER_AUTHORIZATION) String bearerToken);
}
