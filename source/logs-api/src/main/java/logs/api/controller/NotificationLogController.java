package logs.api.controller;

import logs.api.dto.ApiMessageDto;
import logs.api.dto.ResponseListDto;
import logs.api.dto.notificationLog.NotificationLogDto;
import logs.api.mapper.NotificationLogMapper;
import logs.api.model.NotificationLog;
import logs.api.model.criteria.NotificationLogCriteria;
import logs.api.repository.NotificationLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/notification-log")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class NotificationLogController extends ABasicController {

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private NotificationLogMapper notificationLogMapper;

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOL_L')")
    public ApiMessageDto<ResponseListDto<List<NotificationLogDto>>> list(NotificationLogCriteria notificationLogCriteria,
                                                                          @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationLog> page = notificationLogRepository.findAll(notificationLogCriteria.getCriteria(), pageable);
        ResponseListDto<List<NotificationLogDto>> responseListDto =
                makeResponseListDto(page, notificationLogMapper::fromEntityToNotificationLogDtoList);
        return makeSuccessResponse(responseListDto, "Get list success");
    }
}
