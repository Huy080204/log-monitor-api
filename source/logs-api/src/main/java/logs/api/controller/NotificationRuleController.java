package logs.api.controller;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.ResponseListDto;
import logs.api.dto.notificationRule.NotificationRuleDto;
import logs.api.dto.notificationRule.NotificationRuleItemDto;
import logs.api.exception.BadRequestException;
import logs.api.exception.NotFoundException;
import logs.api.form.notificationRule.CreateNotificationRuleForm;
import logs.api.form.notificationRule.NotificationRuleItemForm;
import logs.api.form.notificationRule.UpdateNotificationRuleForm;
import logs.api.form.notificationRule.UpdateNotificationRuleItemForm;
import logs.api.mapper.NotificationRuleItemMapper;
import logs.api.mapper.NotificationRuleMapper;
import logs.api.model.Applications;
import logs.api.model.NotificationGroup;
import logs.api.model.NotificationRule;
import logs.api.model.NotificationRuleItem;
import logs.api.model.QueryTemplate;
import logs.api.model.criteria.NotificationRuleCriteria;
import logs.api.repository.ApplicationsRepository;
import logs.api.repository.NotificationGroupRepository;
import logs.api.repository.NotificationRuleItemRepository;
import logs.api.repository.NotificationRuleRepository;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private NotificationRuleItemRepository notificationRuleItemRepository;

    @Autowired
    private ApplicationsRepository applicationsRepository;

    @Autowired
    private QueryTemplateRepository queryTemplateRepository;

    @Autowired
    private NotificationRuleMapper notificationRuleMapper;

    @Autowired
    private NotificationRuleItemMapper notificationRuleItemMapper;

    @GetMapping(value = "/get/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NTR_V')")
    public ApiMessageDto<NotificationRuleDto> get(@PathVariable Long id) {
        NotificationRule notificationRule = notificationRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found notification rule", ErrorCode.NOTIFICATION_RULE_ERROR_NOT_FOUND));
        NotificationRuleDto notificationRuleDto = notificationRuleMapper.fromEntityToNotificationRuleDto(notificationRule);
        List<NotificationRuleItem> items = notificationRuleItemRepository.findAllByNotificationRuleIdOrderByOrdering(id);
        List<NotificationRuleItemDto> itemDtos = notificationRuleItemMapper.fromEntityToNotificationRuleItemDtoList(items);
        notificationRuleDto.setItems(itemDtos);
        return makeSuccessResponse(notificationRuleDto, "Get notification rule success");
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NTR_L')")
    public ApiMessageDto<ResponseListDto<List<NotificationRuleDto>>> list(NotificationRuleCriteria notificationRuleCriteria, @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationRule> page = notificationRuleRepository.findAll(notificationRuleCriteria.getCriteria(), pageable);
        ResponseListDto<List<NotificationRuleDto>> responseListDto =
                makeResponseListDto(page, notificationRuleMapper::fromEntityToNotificationRuleDtoList);

        List<Long> notificationRuleIds = page.getContent().stream().map(NotificationRule::getId).collect(Collectors.toList());
        if (!notificationRuleIds.isEmpty()) {
            Map<Long, List<NotificationRuleItem>> itemsByRuleId = notificationRuleItemRepository.findAllByNotificationRuleIdInFetchingRefs(notificationRuleIds).stream()
                    .collect(Collectors.groupingBy(notificationRuleItem -> notificationRuleItem.getNotificationRule().getId()));
            for (NotificationRuleDto notificationRuleDto : responseListDto.getContent()) {
                List<NotificationRuleItem> items = itemsByRuleId.getOrDefault(notificationRuleDto.getId(), Collections.emptyList());
                notificationRuleDto.setItems(notificationRuleItemMapper.fromEntityToNotificationRuleItemDtoList(items));
            }
        }

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

        List<NotificationRuleItem> notificationRuleItems = resolveNewItems(createNotificationRuleForm.getItems());

        NotificationRule notificationRule = notificationRuleMapper.fromFormToEntity(createNotificationRuleForm);
        notificationRule.setNotificationGroup(notificationGroup);
        notificationRuleRepository.save(notificationRule);

        for (NotificationRuleItem notificationRuleItem : notificationRuleItems) {
            notificationRuleItem.setNotificationRule(notificationRule);
        }
        notificationRuleItemRepository.saveAll(notificationRuleItems);

        return makeSuccessResponse(notificationRuleMapper.fromEntityToNotificationRuleIdDto(notificationRule), "Create notification rule success");
    }

    @PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NTR_U')")
    @Transactional
    public ApiMessageDto<Void> update(@Valid @RequestBody UpdateNotificationRuleForm updateNotificationRuleForm, BindingResult bindingResult) {
        NotificationRule notificationRule = notificationRuleRepository.findById(updateNotificationRuleForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found notification rule", ErrorCode.NOTIFICATION_RULE_ERROR_NOT_FOUND));

        if (!Objects.equals(updateNotificationRuleForm.getName(), notificationRule.getName())
                && notificationRuleRepository.existsByNotificationGroupIdAndNameAndIdNot(notificationRule.getNotificationGroup().getId(), updateNotificationRuleForm.getName(), updateNotificationRuleForm.getId())) {
            throw new BadRequestException("Notification rule name existed in group", ErrorCode.NOTIFICATION_RULE_ERROR_NAME_EXISTED);
        }

        notificationRuleMapper.updateEntityFromForm(updateNotificationRuleForm, notificationRule);
        notificationRuleRepository.save(notificationRule);
        applyItemChanges(notificationRule, updateNotificationRuleForm.getItems());

        return makeSuccessResponse("Update notification rule success");
    }

    private List<NotificationRuleItem> resolveNewItems(List<NotificationRuleItemForm> itemForms) {
        Map<Long, Applications> applicationsById = resolveApplications(itemForms.stream()
                .map(NotificationRuleItemForm::getApplicationId).collect(Collectors.toList()));
        Map<Long, QueryTemplate> queryTemplateById = resolveQueryTemplates(itemForms.stream()
                .map(NotificationRuleItemForm::getQueryTemplateId).collect(Collectors.toList()));

        List<NotificationRuleItem> notificationRuleItems = new ArrayList<>();
        for (int index = 0; index < itemForms.size(); index++) {
            NotificationRuleItemForm itemForm = itemForms.get(index);
            Applications application = applicationsById.get(itemForm.getApplicationId());
            QueryTemplate queryTemplate = queryTemplateById.get(itemForm.getQueryTemplateId());
            validateApplicationMatch(application, queryTemplate);

            NotificationRuleItem notificationRuleItem = new NotificationRuleItem();
            notificationRuleItem.setApplication(application);
            notificationRuleItem.setQueryTemplate(queryTemplate);
            notificationRuleItem.setOrdering(index);
            notificationRuleItem.setOperator(index == 0 ? null : itemForm.getOperator());
            notificationRuleItems.add(notificationRuleItem);
        }
        return notificationRuleItems;
    }

    private void applyItemChanges(NotificationRule notificationRule, List<UpdateNotificationRuleItemForm> itemForms) {
        Map<Long, Applications> applicationsById = resolveApplications(itemForms.stream()
                .map(UpdateNotificationRuleItemForm::getApplicationId).collect(Collectors.toList()));
        Map<Long, QueryTemplate> queryTemplateById = resolveQueryTemplates(itemForms.stream()
                .map(UpdateNotificationRuleItemForm::getQueryTemplateId).collect(Collectors.toList()));
        Map<Long, NotificationRuleItem> existingItemsById = notificationRuleItemRepository.findAllByNotificationRuleIdOrderByOrdering(notificationRule.getId()).stream()
                .collect(Collectors.toMap(NotificationRuleItem::getId, notificationRuleItem -> notificationRuleItem));

        List<NotificationRuleItem> itemsToSave = new ArrayList<>();
        for (int index = 0; index < itemForms.size(); index++) {
            UpdateNotificationRuleItemForm itemForm = itemForms.get(index);
            Applications application = applicationsById.get(itemForm.getApplicationId());
            QueryTemplate queryTemplate = queryTemplateById.get(itemForm.getQueryTemplateId());
            validateApplicationMatch(application, queryTemplate);

            NotificationRuleItem notificationRuleItem;
            if (itemForm.getId() != null) {
                notificationRuleItem = existingItemsById.remove(itemForm.getId());
                if (notificationRuleItem == null) {
                    throw new NotFoundException("Not found notification rule item", ErrorCode.NOTIFICATION_RULE_ERROR_ITEM_NOT_FOUND);
                }
            } else {
                notificationRuleItem = new NotificationRuleItem();
                notificationRuleItem.setNotificationRule(notificationRule);
            }
            notificationRuleItem.setApplication(application);
            notificationRuleItem.setQueryTemplate(queryTemplate);
            notificationRuleItem.setOrdering(index);
            notificationRuleItem.setOperator(index == 0 ? null : itemForm.getOperator());
            itemsToSave.add(notificationRuleItem);
        }

        if (!existingItemsById.isEmpty()) {
            notificationRuleItemRepository.deleteAllByIdIn(existingItemsById.keySet());
        }
        notificationRuleItemRepository.saveAll(itemsToSave);
    }

    private Map<Long, Applications> resolveApplications(List<Long> applicationIds) {
        List<Long> distinctIds = applicationIds.stream().distinct().collect(Collectors.toList());
        Map<Long, Applications> applicationsById = applicationsRepository.findAllById(distinctIds).stream()
                .collect(Collectors.toMap(Applications::getId, applications -> applications, (a, b) -> a));
        if (applicationsById.size() != distinctIds.size()) {
            throw new NotFoundException("Not found application", ErrorCode.APPLICATIONS_ERROR_NOT_FOUND);
        }
        return applicationsById;
    }

    private Map<Long, QueryTemplate> resolveQueryTemplates(List<Long> queryTemplateIds) {
        List<Long> distinctIds = queryTemplateIds.stream().distinct().collect(Collectors.toList());
        Map<Long, QueryTemplate> queryTemplateById = queryTemplateRepository.findAllById(distinctIds).stream()
                .collect(Collectors.toMap(QueryTemplate::getId, queryTemplate -> queryTemplate, (a, b) -> a));
        if (queryTemplateById.size() != distinctIds.size()) {
            throw new NotFoundException("Not found query template", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND);
        }
        return queryTemplateById;
    }

    private void validateApplicationMatch(Applications application, QueryTemplate queryTemplate) {
        if (queryTemplate.getApplication() != null && !Objects.equals(queryTemplate.getApplication().getId(), application.getId())) {
            throw new BadRequestException("Query template application mismatch", ErrorCode.NOTIFICATION_GROUP_ERROR_CHECK_TYPE_MISMATCH);
        }
    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NTR_D')")
    @Transactional
    public ApiMessageDto<Void> delete(@PathVariable Long id) {
        NotificationRule notificationRule = notificationRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found notification rule", ErrorCode.NOTIFICATION_RULE_ERROR_NOT_FOUND));
        notificationRuleItemRepository.deleteAllByNotificationRuleId(id);
        notificationRuleRepository.delete(notificationRule);
        return makeSuccessResponse("Delete notification rule success");
    }
}
