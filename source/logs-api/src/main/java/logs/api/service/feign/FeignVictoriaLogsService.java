package logs.api.service.feign;

import logs.api.config.CustomFeignConfig;
import logs.api.config.VictoriaLogsFeignConfig;
import logs.api.dto.victorialogs.VictoriaLogsQueryForm;
import logs.api.dto.victorialogs.VictoriaLogsStatsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(
        name = "victoria-logs-svr",
        url = "${victorialogs.api.url}",
        configuration = {CustomFeignConfig.class, VictoriaLogsFeignConfig.class})
public interface FeignVictoriaLogsService {

    @PostMapping(
            value = "/select/logsql/query",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    List<VictoriaLogsStatsDto> query(@RequestHeader(FeignSSOService.LOGIN_TYPE) String loginType,
                                      @RequestBody VictoriaLogsQueryForm form);
}
