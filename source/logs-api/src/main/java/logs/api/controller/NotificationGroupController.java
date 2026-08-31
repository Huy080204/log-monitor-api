package logs.api.controller;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.ResponseListDto;
import logs.api.dto.notificationGroup.NotificationGroupDto;
import logs.api.dto.setting.SettingNotificationChannelDto;
import logs.api.exception.BadRequestException;
import logs.api.exception.NotFoundException;
import logs.api.form.notificationGroup.CreateNotificationGroupForm;
import logs.api.form.notificationGroup.UpdateNotificationGroupForm;
import logs.api.mapper.NotificationGroupMapper;
import logs.api.model.NotificationGroup;
import logs.api.model.criteria.NotificationGroupCriteria;
import logs.api.repository.NotificationGroupRepository;
import logs.api.repository.NotificationQueryRepository;
import logs.api.repository.NotificationRepository;
import logs.api.service.NotificationService;
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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/v1/notification-group")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class NotificationGroupController extends ABasicController {
    @Autowired
    private NotificationGroupRepository notificationGroupRepository;

    @Autowired
    private NotificationGroupMapper notificationGroupMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationQueryRepository notificationQueryRepository;

    @Autowired
    private NotificationService notificationService;

    @GetMapping(value = "/get/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOG_V')")
    public ApiMessageDto<NotificationGroupDto> get(@PathVariable Long id) {
        NotificationGroup notificationGroup = notificationGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found notification group", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND));
        return makeSuccessResponse(notificationGroupMapper.fromEntityToNotificationGroupDto(notificationGroup), "Get notification group success");
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOG_L')")
    public ApiMessageDto<ResponseListDto<List<NotificationGroupDto>>> list(NotificationGroupCriteria notificationGroupCriteria, @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationGroup> page = notificationGroupRepository.findAll(notificationGroupCriteria.getCriteria(), pageable);
        ResponseListDto<List<NotificationGroupDto>> responseListDto =
                makeResponseListDto(page, notificationGroupMapper::fromEntityListToNotificationGroupDtoList);
        return makeSuccessResponse(responseListDto, "Get list notification group success");
    }

    @GetMapping(value = "/auto-complete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<ResponseListDto<List<NotificationGroupDto>>> autoComplete(NotificationGroupCriteria notificationGroupCriteria, Pageable pageable) {
        notificationGroupCriteria.setStatus(BaseConstant.STATUS_ACTIVE);
        Page<NotificationGroup> page = notificationGroupRepository.findAll(notificationGroupCriteria.getCriteria(), pageable);
        return makeSuccessResponse(makeResponseListDto(page, notificationGroupMapper::fromEntityListToNotificationGroupDtoAutoCompleteList), "Get auto complete notification groups success");
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOG_C')")
    @Transactional
    public ApiMessageDto<NotificationGroupDto> create(@Valid @RequestBody CreateNotificationGroupForm createNotificationGroupForm, BindingResult bindingResult) {
        if (notificationGroupRepository.existsByName(createNotificationGroupForm.getName())) {
            throw new BadRequestException("Notification group name existed", ErrorCode.NOTIFICATION_GROUP_ERROR_NAME_EXISTED);
        }

        NotificationGroup notificationGroup = notificationGroupMapper.fromFormToEntity(createNotificationGroupForm);
        if (createNotificationGroupForm.getType() == null) {
            SettingNotificationChannelDto groupSetting = notificationService.parseChannelSetting(notificationGroup.getChannelSetting());
            notificationGroup.setType(groupSetting.getType());
        }
        notificationGroup.setStatus(BaseConstant.STATUS_PENDING);
        notificationGroupRepository.save(notificationGroup);
        return makeSuccessResponse(notificationGroupMapper.fromEntityToNotificationGroupIdDto(notificationGroup), "Create notification group success");
    }

    @PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOG_U')")
    @Transactional
    public ApiMessageDto<Void> update(@Valid @RequestBody UpdateNotificationGroupForm updateNotificationGroupForm, BindingResult bindingResult) {
        NotificationGroup notificationGroup = notificationGroupRepository.findById(updateNotificationGroupForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found notification group", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND));

        if (!notificationGroup.getName().equals(updateNotificationGroupForm.getName())
                && notificationGroupRepository.existsByName(updateNotificationGroupForm.getName())) {
            throw new BadRequestException("Notification group name existed", ErrorCode.NOTIFICATION_GROUP_ERROR_NAME_EXISTED);
        }

        notificationGroupMapper.updateEntityFromForm(updateNotificationGroupForm, notificationGroup);
        if (updateNotificationGroupForm.getType() == null) {
            SettingNotificationChannelDto groupSetting = notificationService.parseChannelSetting(notificationGroup.getChannelSetting());
            notificationGroup.setType(groupSetting.getType());
        }
        notificationGroupRepository.save(notificationGroup);
        return makeSuccessResponse("Update notification group success");
    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOG_D')")
    @Transactional
    public ApiMessageDto<Void> delete(@PathVariable Long id) {
        NotificationGroup notificationGroup = notificationGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found notification group", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND));

        if (BaseConstant.STATUS_ACTIVE.equals(notificationGroup.getStatus())) {
            throw new BadRequestException("Cannot delete an active notification group", ErrorCode.NOTIFICATION_GROUP_ERROR_DELETE_ACTIVE);
        }

        notificationRepository.deleteAllByNotificationGroupId(id);
        notificationQueryRepository.deleteAllByNotificationGroupId(id);
        notificationGroupRepository.delete(notificationGroup);
        return makeSuccessResponse("Delete notification group success");
    }

    @PutMapping(value = "/activate/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOG_U')")
    @Transactional
    public ApiMessageDto<Void> activate(@PathVariable Long id) {
        NotificationGroup notificationGroup = notificationGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found notification group", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND));

        notificationGroupRepository.findFirstByStatus(BaseConstant.STATUS_ACTIVE)
                .filter(currentActive -> !currentActive.getId().equals(id))
                .ifPresent(currentActive -> {
                    currentActive.setStatus(BaseConstant.STATUS_PENDING);
                    notificationGroupRepository.save(currentActive);
                });

        notificationGroup.setStatus(BaseConstant.STATUS_ACTIVE);
        notificationGroupRepository.save(notificationGroup);
        return makeSuccessResponse("Activate notification group success");
    }
}
