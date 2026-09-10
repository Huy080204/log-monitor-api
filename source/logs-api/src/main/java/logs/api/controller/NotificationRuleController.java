package logs.api.controller;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.ResponseListDto;
import logs.api.dto.notificationRule.NotificationRuleDto;
import logs.api.exception.BadRequestException;
import logs.api.exception.NotFoundException;
import logs.api.form.notificationRule.CreateNotificationRuleForm;
import logs.api.form.notificationRule.UpdateNotificationRuleForm;
import logs.api.mapper.NotificationRuleMapper;
import logs.api.model.NotificationGroup;
import logs.api.model.NotificationRule;
import logs.api.model.criteria.NotificationRuleCriteria;
import logs.api.repository.NotificationGroupRepository;
import logs.api.repository.NotificationRuleRepository;
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
import java.util.Objects;

@RestController
@RequestMapping("/v1/notification-rule")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class NotificationRuleController extends ABasicController {
    @Autowired
    private NotificationRuleRepository notificationRuleRepository;

    @Autowired
    private NotificationGroupRepository notificationGroupRepository;

    @Autowired
    private NotificationRuleMapper notificationRuleMapper;

    @GetMapping(value = "/get/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NTR_V')")
    public ApiMessageDto<NotificationRuleDto> get(@PathVariable Long id) {
        NotificationRule notificationRule = notificationRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found notification rule", ErrorCode.NOTIFICATION_RULE_ERROR_NOT_FOUND));
        return makeSuccessResponse(notificationRuleMapper.fromEntityToNotificationRuleDto(notificationRule), "Get notification rule success");
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NTR_L')")
    public ApiMessageDto<ResponseListDto<List<NotificationRuleDto>>> list(NotificationRuleCriteria notificationRuleCriteria, @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationRule> page = notificationRuleRepository.findAll(notificationRuleCriteria.getCriteria(), pageable);
        ResponseListDto<List<NotificationRuleDto>> responseListDto =
                makeResponseListDto(page, notificationRuleMapper::fromEntityToNotificationRuleDtoList);
        return makeSuccessResponse(responseListDto, "Get list success");
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NTR_C')")
    @Transactional
    public ApiMessageDto<NotificationRuleDto> create(@Valid @RequestBody CreateNotificationRuleForm createNotificationRuleForm, BindingResult bindingResult) {
        NotificationGroup notificationGroup = notificationGroupRepository.findById(createNotificationRuleForm.getNotificationGroupId())
                .orElseThrow(() -> new NotFoundException("Not found notification group", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND));

        if (!BaseConstant.NOTIFICATION_GROUP_CHECK_TYPE_COMPARISON.equals(notificationGroup.getCheckType())) {
            throw new BadRequestException("Notification group check type mismatch", ErrorCode.NOTIFICATION_GROUP_ERROR_CHECK_TYPE_MISMATCH);
        }
        if (notificationRuleRepository.existsByNotificationGroupIdAndName(createNotificationRuleForm.getNotificationGroupId(), createNotificationRuleForm.getName())) {
            throw new BadRequestException("Notification rule name existed in group", ErrorCode.NOTIFICATION_RULE_ERROR_NAME_EXISTED);
        }

        NotificationRule notificationRule = notificationRuleMapper.fromFormToEntity(createNotificationRuleForm);
        notificationRule.setNotificationGroup(notificationGroup);
        notificationRuleRepository.save(notificationRule);
        return makeSuccessResponse(notificationRuleMapper.fromEntityToNotificationRuleIdDto(notificationRule), "Create notification rule success");
    }

    @PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NTR_U')")
    @Transactional
    public ApiMessageDto<Void> update(@Valid @RequestBody UpdateNotificationRuleForm updateNotificationRuleForm, BindingResult bindingResult) {
        NotificationRule notificationRule = notificationRuleRepository.findById(updateNotificationRuleForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found notification rule", ErrorCode.NOTIFICATION_RULE_ERROR_NOT_FOUND));

        if (!BaseConstant.NOTIFICATION_GROUP_CHECK_TYPE_COMPARISON.equals(notificationRule.getNotificationGroup().getCheckType())) {
            throw new BadRequestException("Notification group check type mismatch", ErrorCode.NOTIFICATION_GROUP_ERROR_CHECK_TYPE_MISMATCH);
        }
        if (!Objects.equals(updateNotificationRuleForm.getName(), notificationRule.getName())
                && notificationRuleRepository.existsByNotificationGroupIdAndNameAndIdNot(notificationRule.getNotificationGroup().getId(), updateNotificationRuleForm.getName(), updateNotificationRuleForm.getId())) {
            throw new BadRequestException("Notification rule name existed in group", ErrorCode.NOTIFICATION_RULE_ERROR_NAME_EXISTED);
        }

        notificationRuleMapper.updateEntityFromForm(updateNotificationRuleForm, notificationRule);
        notificationRuleRepository.save(notificationRule);
        return makeSuccessResponse("Update notification rule success");
    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NTR_D')")
    @Transactional
    public ApiMessageDto<Void> delete(@PathVariable Long id) {
        NotificationRule notificationRule = notificationRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found notification rule", ErrorCode.NOTIFICATION_RULE_ERROR_NOT_FOUND));
        notificationRuleRepository.delete(notificationRule);
        return makeSuccessResponse("Delete notification rule success");
    }
}
