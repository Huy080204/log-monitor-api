package logs.api.controller;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.ResponseListDto;
import logs.api.dto.notification.NotificationDto;
import logs.api.exception.NotFoundException;
import logs.api.form.notification.ReadNotificationForm;
import logs.api.mapper.NotificationMapper;
import logs.api.model.Notification;
import logs.api.model.criteria.NotificationCriteria;
import logs.api.repository.NotificationRepository;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/v1/notification")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class NotificationController extends ABasicController {
    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationMapper notificationMapper;

    @GetMapping(value = "/get/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOTI_V')")
    public ApiMessageDto<NotificationDto> get(@PathVariable Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found notification", ErrorCode.NOTIFICATION_ERROR_NOT_FOUND));
        return makeSuccessResponse(notificationMapper.fromEntityToNotificationDto(notification), "Get notification success");
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOTI_L')")
    public ApiMessageDto<ResponseListDto<List<NotificationDto>>> list(NotificationCriteria notificationCriteria, @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Notification> page = notificationRepository.findAll(notificationCriteria.getCriteria(), pageable);
        ResponseListDto<List<NotificationDto>> responseListDto =
                makeResponseListDto(page, notificationMapper::fromEntityListToNotificationDtoList);
        return makeSuccessResponse(responseListDto, "Get list notification success");
    }

    @GetMapping(value = "/auto-complete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<ResponseListDto<List<NotificationDto>>> autoComplete(NotificationCriteria notificationCriteria, Pageable pageable) {
        Page<Notification> page = notificationRepository.findAll(notificationCriteria.getCriteria(), pageable);
        return makeSuccessResponse(makeResponseListDto(page, notificationMapper::fromEntityListToNotificationDtoList), "Get auto complete notifications success");
    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOTI_D')")
    @Transactional
    public ApiMessageDto<Void> delete(@PathVariable Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found notification", ErrorCode.NOTIFICATION_ERROR_NOT_FOUND));
        notificationRepository.delete(notification);
        return makeSuccessResponse("Delete notification success");
    }

    @PutMapping(value = "/read", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ApiMessageDto<Void> read(@Valid @RequestBody ReadNotificationForm readNotificationForm, BindingResult bindingResult) {
        Notification notification = notificationRepository.findById(readNotificationForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found notification", ErrorCode.NOTIFICATION_ERROR_NOT_FOUND));

        if (BaseConstant.NOTIFICATION_STATE_SENT.equals(notification.getState())) {
            notification.setState(BaseConstant.NOTIFICATION_STATE_READ);
            notificationRepository.save(notification);
        }
        return makeSuccessResponse("Read notification success");
    }
}
