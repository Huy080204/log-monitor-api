package logs.api.controller;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.ResponseListDto;
import logs.api.dto.notificationChannel.NotificationChannelDto;
import logs.api.exception.BadRequestException;
import logs.api.exception.NotFoundException;
import logs.api.form.notificationChannel.CreateNotificationChannelForm;
import logs.api.form.notificationChannel.UpdateNotificationChannelForm;
import logs.api.mapper.NotificationChannelMapper;
import logs.api.model.NotificationChannel;
import logs.api.model.criteria.NotificationChannelCriteria;
import logs.api.repository.NotificationChannelRepository;
import logs.api.repository.NotificationGroupRepository;
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
@RequestMapping("/v1/notification-channel")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class NotificationChannelController extends ABasicController {
    @Autowired
    private NotificationChannelRepository notificationChannelRepository;

    @Autowired
    private NotificationGroupRepository notificationGroupRepository;

    @Autowired
    private NotificationChannelMapper notificationChannelMapper;

    @GetMapping(value = "/get/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOC_V')")
    public ApiMessageDto<NotificationChannelDto> get(@PathVariable Long id) {
        NotificationChannel notificationChannel = notificationChannelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found notification channel!", ErrorCode.NOTIFICATION_CHANNEL_ERROR_NOT_FOUND));
        return makeSuccessResponse(notificationChannelMapper.fromEntityToNotificationChannelDto(notificationChannel), "Get notification channel success");
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOC_L')")
    public ApiMessageDto<ResponseListDto<List<NotificationChannelDto>>> list(NotificationChannelCriteria notificationChannelCriteria, @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationChannel> page = notificationChannelRepository.findAll(notificationChannelCriteria.getCriteria(), pageable);
        ResponseListDto<List<NotificationChannelDto>> responseListDto =
                makeResponseListDto(page, notificationChannelMapper::fromEntityToNotificationChannelDtoList);
        return makeSuccessResponse(responseListDto, "Get list success");
    }

    @GetMapping(value = "/auto-complete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<ResponseListDto<List<NotificationChannelDto>>> autoComplete(NotificationChannelCriteria notificationChannelCriteria, Pageable pageable) {
        notificationChannelCriteria.setStatus(BaseConstant.STATUS_ACTIVE);
        Page<NotificationChannel> page = notificationChannelRepository.findAll(notificationChannelCriteria.getCriteria(), pageable);
        ResponseListDto<List<NotificationChannelDto>> responseListDto =
                makeResponseListDto(page, notificationChannelMapper::fromEntityListToNotificationChannelDtoAutoComplete);
        return makeSuccessResponse(responseListDto, "Get auto complete notification channels success");
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOC_C')")
    @Transactional
    public ApiMessageDto<NotificationChannelDto> create(@Valid @RequestBody CreateNotificationChannelForm createNotificationChannelForm, BindingResult bindingResult) {
        if (notificationChannelRepository.existsByName(createNotificationChannelForm.getName())) {
            throw new BadRequestException("Notification channel already exist", ErrorCode.NOTIFICATION_CHANNEL_ERROR_NAME_EXISTED);
        }

        NotificationChannel notificationChannel = notificationChannelMapper.fromFormToEntity(createNotificationChannelForm);
        notificationChannelRepository.save(notificationChannel);
        return makeSuccessResponse(notificationChannelMapper.fromEntityToNotificationChannelIdDto(notificationChannel), "Create notification channel success");
    }

    @PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOC_U')")
    @Transactional
    public ApiMessageDto<Void> update(@Valid @RequestBody UpdateNotificationChannelForm updateNotificationChannelForm, BindingResult bindingResult) {
        NotificationChannel notificationChannel = notificationChannelRepository.findById(updateNotificationChannelForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found notification channel!", ErrorCode.NOTIFICATION_CHANNEL_ERROR_NOT_FOUND));

        if (!notificationChannel.getName().equals(updateNotificationChannelForm.getName())
                && notificationChannelRepository.existsByName(updateNotificationChannelForm.getName())) {
            throw new BadRequestException("Notification channel already exist", ErrorCode.NOTIFICATION_CHANNEL_ERROR_NAME_EXISTED);
        }
        notificationChannelMapper.updateEntityFromForm(updateNotificationChannelForm, notificationChannel);
        notificationChannelRepository.save(notificationChannel);
        return makeSuccessResponse("Update notification channel success");
    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOC_D')")
    @Transactional
    public ApiMessageDto<Void> delete(@PathVariable Long id) {
        NotificationChannel notificationChannel = notificationChannelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found notification channel!", ErrorCode.NOTIFICATION_CHANNEL_ERROR_NOT_FOUND));

        if (notificationGroupRepository.existsByNotificationChannelId(id)) {
            throw new BadRequestException("Notification channel is in use", ErrorCode.NOTIFICATION_CHANNEL_ERROR_IN_USE);
        }

        notificationChannelRepository.delete(notificationChannel);
        return makeSuccessResponse("Delete notification channel success");
    }
}
