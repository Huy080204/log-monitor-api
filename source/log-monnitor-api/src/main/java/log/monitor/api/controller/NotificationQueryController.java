package log.monitor.api.controller;

import log.monitor.api.dto.ApiMessageDto;
import log.monitor.api.dto.ErrorCode;
import log.monitor.api.dto.ResponseListDto;
import log.monitor.api.dto.notificationQuery.NotificationQueryDto;
import log.monitor.api.exception.BadRequestException;
import log.monitor.api.exception.NotFoundException;
import log.monitor.api.form.notificationQuery.CreateNotificationQueryForm;
import log.monitor.api.form.notificationQuery.UpdateNotificationQueryForm;
import log.monitor.api.mapper.NotificationQueryMapper;
import log.monitor.api.model.NotificationGroup;
import log.monitor.api.model.NotificationQuery;
import log.monitor.api.model.criteria.NotificationQueryCriteria;
import log.monitor.api.repository.NotificationGroupRepository;
import log.monitor.api.repository.NotificationQueryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/v1/notification-query")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class NotificationQueryController extends ABasicController {
    @Autowired
    private NotificationQueryRepository notificationQueryRepository;

    @Autowired
    private NotificationGroupRepository notificationGroupRepository;

    @Autowired
    private NotificationQueryMapper notificationQueryMapper;

    @GetMapping(value = "/get/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOQ_V')")
    public ApiMessageDto<NotificationQueryDto> get(@PathVariable Long id) {
        NotificationQuery notificationQuery = notificationQueryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found notification query", ErrorCode.NOTIFICATION_QUERY_ERROR_NOT_FOUND));
        return makeSuccessResponse(notificationQueryMapper.fromEntityToNotificationQueryDto(notificationQuery), "Get notification query success");
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOQ_L')")
    public ApiMessageDto<ResponseListDto<List<NotificationQueryDto>>> list(NotificationQueryCriteria notificationQueryCriteria, @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationQuery> page = notificationQueryRepository.findAll(notificationQueryCriteria.getCriteria(), pageable);
        ResponseListDto<List<NotificationQueryDto>> responseListDto =
                makeResponseListDto(page, notificationQueryMapper::fromEntityListToNotificationQueryDtoList);
        return makeSuccessResponse(responseListDto, "Get list notification query success");
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOQ_C')")
    @Transactional
    public ApiMessageDto<NotificationQueryDto> create(@Valid @RequestBody CreateNotificationQueryForm createNotificationQueryForm, BindingResult bindingResult) {
        NotificationGroup notificationGroup = notificationGroupRepository.findById(createNotificationQueryForm.getNotificationGroupId())
                .orElseThrow(() -> new NotFoundException("Not found notification group", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND));

        if (notificationQueryRepository.existsByNameAndNotificationGroupId(createNotificationQueryForm.getName(), createNotificationQueryForm.getNotificationGroupId())) {
            throw new BadRequestException("Notification query name existed for this group", ErrorCode.NOTIFICATION_QUERY_ERROR_NAME_EXISTED);
        }

        if (notificationQueryRepository.existsByQueryAndNotificationGroupId(createNotificationQueryForm.getQuery(), createNotificationQueryForm.getNotificationGroupId())) {
            throw new BadRequestException("Notification query existed for this group", ErrorCode.NOTIFICATION_QUERY_ERROR_EXISTED);
        }

        NotificationQuery notificationQuery = notificationQueryMapper.fromFormToEntity(createNotificationQueryForm);
        notificationQuery.setNotificationGroup(notificationGroup);
        notificationQueryRepository.save(notificationQuery);
        return makeSuccessResponse(notificationQueryMapper.fromEntityToNotificationQueryIdDto(notificationQuery), "Create notification query success");
    }

    @PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOQ_U')")
    @Transactional
    public ApiMessageDto<Void> update(@Valid @RequestBody UpdateNotificationQueryForm updateNotificationQueryForm, BindingResult bindingResult) {
        NotificationQuery notificationQuery = notificationQueryRepository.findById(updateNotificationQueryForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found notification query", ErrorCode.NOTIFICATION_QUERY_ERROR_NOT_FOUND));

        if (!Objects.equals(notificationQuery.getName(), updateNotificationQueryForm.getName())
                && notificationQueryRepository.existsByNameAndNotificationGroupId(updateNotificationQueryForm.getName(), notificationQuery.getNotificationGroup().getId())) {
            throw new BadRequestException("Notification query name existed for this group", ErrorCode.NOTIFICATION_QUERY_ERROR_NAME_EXISTED);
        }

        if (!Objects.equals(notificationQuery.getQuery(), updateNotificationQueryForm.getQuery())
                && notificationQueryRepository.existsByQueryAndNotificationGroupId(updateNotificationQueryForm.getQuery(), notificationQuery.getNotificationGroup().getId())) {
            throw new BadRequestException("Notification query existed for this group", ErrorCode.NOTIFICATION_QUERY_ERROR_EXISTED);
        }

        notificationQueryMapper.updateEntityFromForm(updateNotificationQueryForm, notificationQuery);
        notificationQueryRepository.save(notificationQuery);
        return makeSuccessResponse("Update notification query success");
    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOQ_D')")
    @Transactional
    public ApiMessageDto<Void> delete(@PathVariable Long id) {
        NotificationQuery notificationQuery = notificationQueryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found notification query", ErrorCode.NOTIFICATION_QUERY_ERROR_NOT_FOUND));
        notificationQueryRepository.delete(notificationQuery);
        return makeSuccessResponse("Delete notification query success");
    }
}
