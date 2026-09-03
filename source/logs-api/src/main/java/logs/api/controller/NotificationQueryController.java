package logs.api.controller;

import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.ResponseListDto;
import logs.api.dto.notificationQuery.NotificationQueryDto;
import logs.api.exception.BadRequestException;
import logs.api.exception.NotFoundException;
import logs.api.form.notificationQuery.CreateNotificationQueryForm;
import logs.api.form.notificationQuery.UpdateNotificationQueryForm;
import logs.api.mapper.NotificationQueryMapper;
import logs.api.model.NotificationGroup;
import logs.api.model.NotificationQuery;
import logs.api.model.QueryTemplate;
import logs.api.model.criteria.NotificationQueryCriteria;
import logs.api.repository.NotificationGroupRepository;
import logs.api.repository.NotificationQueryRepository;
import logs.api.repository.QueryTemplateRepository;
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
    private QueryTemplateRepository queryTemplateRepository;

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

        QueryTemplate queryTemplate = queryTemplateRepository.findById(createNotificationQueryForm.getQueryTemplateId())
                .orElseThrow(() -> new NotFoundException("Not found query template", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND));

        if (notificationQueryRepository.existsByNotificationGroupIdAndQueryTemplateId(createNotificationQueryForm.getNotificationGroupId(), createNotificationQueryForm.getQueryTemplateId())) {
            throw new BadRequestException("Notification query existed for this group", ErrorCode.NOTIFICATION_QUERY_ERROR_EXISTED);
        }

        NotificationQuery notificationQuery = new NotificationQuery();
        notificationQuery.setNotificationGroup(notificationGroup);
        notificationQuery.setQueryTemplate(queryTemplate);
        notificationQueryRepository.save(notificationQuery);
        return makeSuccessResponse(notificationQueryMapper.fromEntityToNotificationQueryIdDto(notificationQuery), "Create notification query success");
    }

    @PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOQ_U')")
    @Transactional
    public ApiMessageDto<Void> update(@Valid @RequestBody UpdateNotificationQueryForm updateNotificationQueryForm, BindingResult bindingResult) {
        NotificationQuery notificationQuery = notificationQueryRepository.findById(updateNotificationQueryForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found notification query", ErrorCode.NOTIFICATION_QUERY_ERROR_NOT_FOUND));

        QueryTemplate queryTemplate = queryTemplateRepository.findById(updateNotificationQueryForm.getQueryTemplateId())
                .orElseThrow(() -> new NotFoundException("Not found query template", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND));

        boolean queryTemplateChanged = !Objects.equals(notificationQuery.getQueryTemplate().getId(), queryTemplate.getId());
        if (queryTemplateChanged && notificationQueryRepository.existsByNotificationGroupIdAndQueryTemplateId(notificationQuery.getNotificationGroup().getId(), queryTemplate.getId())) {
            throw new BadRequestException("Notification query existed for this group", ErrorCode.NOTIFICATION_QUERY_ERROR_EXISTED);
        }

        notificationQueryMapper.updateEntityFromForm(updateNotificationQueryForm, notificationQuery);
        notificationQuery.setQueryTemplate(queryTemplate);
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
