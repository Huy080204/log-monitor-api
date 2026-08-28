package log.monitor.api.service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import log.monitor.api.config.CustomFeignConfig;
import log.monitor.api.dto.ApiMessageDto;
import log.monitor.api.form.file.DeleteListFileForm;

@FeignClient(name = "file-media-svr", url = "${media.internal.base.url}", configuration = CustomFeignConfig.class)
public interface FeignMediaService {
    @PostMapping(value = "/v1/file/delete-list-file")
    ApiMessageDto<String> deleteListFile(@RequestBody DeleteListFileForm deleteListFileForm);
}
