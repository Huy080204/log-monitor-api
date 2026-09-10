package logs.api.controller;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.notificationRule.NotificationRuleDto;
import logs.api.dto.notificationRule.NotificationRuleItemDto;
import logs.api.exception.BadRequestException;
import logs.api.exception.NotFoundException;
import logs.api.form.notificationRule.CreateNotificationRuleForm;
import logs.api.form.notificationRule.NotificationRuleItemForm;
import logs.api.mapper.NotificationRuleItemMapper;
import logs.api.mapper.NotificationRuleMapper;
import logs.api.model.Applications;
import logs.api.model.NotificationGroup;
import logs.api.model.NotificationRule;
import logs.api.model.NotificationRuleItem;
import logs.api.model.QueryTemplate;
import logs.api.repository.ApplicationsRepository;
import logs.api.repository.NotificationGroupRepository;
import logs.api.repository.NotificationRuleItemRepository;
import logs.api.repository.NotificationRuleRepository;
import logs.api.repository.QueryTemplateRepository;
import logs.api.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRuleControllerTest {

    @Mock private NotificationRuleRepository notificationRuleRepository;
    @Mock private NotificationGroupRepository notificationGroupRepository;
    @Mock private NotificationRuleItemRepository notificationRuleItemRepository;
    @Mock private ApplicationsRepository applicationsRepository;
    @Mock private QueryTemplateRepository queryTemplateRepository;
    @Mock private NotificationRuleMapper notificationRuleMapper;
    @Mock private NotificationRuleItemMapper notificationRuleItemMapper;
    @Mock private UserServiceImpl userService;
    @InjectMocks private NotificationRuleController controller;

    private final BindingResult bindingResult = mock(BindingResult.class);

    private static NotificationGroup comparisonGroup(Long id) {
        NotificationGroup group = new NotificationGroup();
        group.setId(id);
        group.setCheckType(BaseConstant.NOTIFICATION_GROUP_CHECK_TYPE_COMPARISON);
        return group;
    }

    private static NotificationRuleItemForm itemForm(Long applicationId, Long queryTemplateId, Integer operator) {
        NotificationRuleItemForm form = new NotificationRuleItemForm();
        form.setApplicationId(applicationId);
        form.setQueryTemplateId(queryTemplateId);
        form.setOperator(operator);
        return form;
    }

    private static Applications application(Long id) {
        Applications applications = new Applications();
        applications.setId(id);
        return applications;
    }

    private static QueryTemplate queryTemplate(Long id, Applications application) {
        QueryTemplate queryTemplate = new QueryTemplate();
        queryTemplate.setId(id);
        queryTemplate.setApplication(application);
        return queryTemplate;
    }

    private static CreateNotificationRuleForm createForm(Long id, Long notificationGroupId, String name, List<NotificationRuleItemForm> items) {
        CreateNotificationRuleForm form = new CreateNotificationRuleForm();
        form.setId(id);
        form.setNotificationGroupId(notificationGroupId);
        form.setName(name);
        form.setItems(items);
        return form;
    }

    @Test
    void shouldCreateNewRuleWhenIdIsNull() {
        CreateNotificationRuleForm form = createForm(null, 1L, "rule-1", List.of(itemForm(10L, 20L, 0)));

        NotificationRule newRule = new NotificationRule();
        NotificationRuleDto idDto = new NotificationRuleDto();

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(comparisonGroup(1L)));
        when(notificationRuleRepository.existsByNotificationGroupIdAndName(1L, "rule-1")).thenReturn(false);
        when(applicationsRepository.findAllById(List.of(10L))).thenReturn(List.of(application(10L)));
        when(queryTemplateRepository.findAllById(List.of(20L))).thenReturn(List.of(queryTemplate(20L, application(10L))));
        when(notificationRuleMapper.fromFormToEntity(form)).thenReturn(newRule);
        when(notificationRuleMapper.fromEntityToNotificationRuleIdDto(newRule)).thenReturn(idDto);

        ApiMessageDto<NotificationRuleDto> result = controller.create(form, bindingResult);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(idDto);
        verify(notificationRuleRepository).save(newRule);
        verify(notificationRuleItemRepository).saveAll(any(List.class));
    }

    @Test
    void shouldUpdateExistingRuleAndReplaceItemsWhenIdMatchesExisting() {
        NotificationRule existingRule = new NotificationRule();
        existingRule.setId(10L);
        existingRule.setNotificationGroup(comparisonGroup(1L));

        CreateNotificationRuleForm form = createForm(10L, 1L, "rule-10-updated",
                List.of(itemForm(10L, 20L, 3), itemForm(10L, 20L, 1)));

        NotificationRuleDto idDto = new NotificationRuleDto();

        when(notificationRuleRepository.findById(10L)).thenReturn(Optional.of(existingRule));
        when(notificationRuleRepository.existsByNotificationGroupIdAndNameAndIdNot(1L, "rule-10-updated", 10L)).thenReturn(false);
        when(applicationsRepository.findAllById(List.of(10L))).thenReturn(List.of(application(10L)));
        when(queryTemplateRepository.findAllById(List.of(20L))).thenReturn(List.of(queryTemplate(20L, application(10L))));
        doAnswer(invocation -> {
            CreateNotificationRuleForm f = invocation.getArgument(0);
            NotificationRule e = invocation.getArgument(1);
            e.setName(f.getName());
            return null;
        }).when(notificationRuleMapper).updateEntityFromForm(any(CreateNotificationRuleForm.class), any(NotificationRule.class));
        when(notificationRuleMapper.fromEntityToNotificationRuleIdDto(existingRule)).thenReturn(idDto);

        ArgumentCaptor<List<NotificationRuleItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);

        ApiMessageDto<NotificationRuleDto> result = controller.create(form, bindingResult);

        assertThat(result.getResult()).isTrue();
        assertThat(existingRule.getName()).isEqualTo("rule-10-updated");
        InOrder inOrder = inOrder(notificationRuleItemRepository);
        inOrder.verify(notificationRuleItemRepository).deleteAllByNotificationRuleId(10L);
        inOrder.verify(notificationRuleItemRepository).saveAll(itemsCaptor.capture());
        List<NotificationRuleItem> savedItems = itemsCaptor.getValue();
        assertThat(savedItems).hasSize(2);
        assertThat(savedItems.get(0).getOrdering()).isEqualTo(0);
        assertThat(savedItems.get(0).getOperator()).isNull();
        assertThat(savedItems.get(1).getOrdering()).isEqualTo(1);
        assertThat(savedItems.get(1).getOperator()).isEqualTo(1);
        verify(notificationRuleRepository).save(existingRule);
    }

    @Test
    void shouldThrowGroupMismatchWhenNotificationGroupIdDoesNotMatchExistingRuleGroup() {
        NotificationRule existingRule = new NotificationRule();
        existingRule.setId(10L);
        existingRule.setNotificationGroup(comparisonGroup(1L));

        CreateNotificationRuleForm form = createForm(10L, 2L, "rule-10", List.of(itemForm(10L, 20L, 0)));

        when(notificationRuleRepository.findById(10L)).thenReturn(Optional.of(existingRule));

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_RULE_ERROR_GROUP_MISMATCH);
    }

    @Test
    void shouldThrowNameExistedWhenCreatingDuplicateNameInGroup() {
        CreateNotificationRuleForm form = createForm(null, 1L, "dup", List.of(itemForm(10L, 20L, 0)));

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(comparisonGroup(1L)));
        when(notificationRuleRepository.existsByNotificationGroupIdAndName(1L, "dup")).thenReturn(true);

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_RULE_ERROR_NAME_EXISTED);
    }

    @Test
    void shouldThrowNameExistedWhenUpdatingToDuplicateNameExcludingSelf() {
        NotificationRule existingRule = new NotificationRule();
        existingRule.setId(10L);
        existingRule.setNotificationGroup(comparisonGroup(1L));

        CreateNotificationRuleForm form = createForm(10L, 1L, "dup", List.of(itemForm(10L, 20L, 0)));

        when(notificationRuleRepository.findById(10L)).thenReturn(Optional.of(existingRule));
        when(notificationRuleRepository.existsByNotificationGroupIdAndNameAndIdNot(1L, "dup", 10L)).thenReturn(true);

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_RULE_ERROR_NAME_EXISTED);
    }

    @Test
    void shouldThrowApplicationNotFoundWhenItemApplicationIdDoesNotExist() {
        CreateNotificationRuleForm form = createForm(null, 1L, "rule-1", List.of(itemForm(10L, 20L, 0)));

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(comparisonGroup(1L)));
        when(notificationRuleRepository.existsByNotificationGroupIdAndName(1L, "rule-1")).thenReturn(false);
        when(applicationsRepository.findAllById(List.of(10L))).thenReturn(List.of());

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.APPLICATIONS_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowQueryTemplateNotFoundWhenItemQueryTemplateIdDoesNotExist() {
        CreateNotificationRuleForm form = createForm(null, 1L, "rule-1", List.of(itemForm(10L, 20L, 0)));

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(comparisonGroup(1L)));
        when(notificationRuleRepository.existsByNotificationGroupIdAndName(1L, "rule-1")).thenReturn(false);
        when(applicationsRepository.findAllById(List.of(10L))).thenReturn(List.of(application(10L)));
        when(queryTemplateRepository.findAllById(List.of(20L))).thenReturn(List.of());

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowApplicationMismatchWhenQueryTemplateApplicationDiffersFromItemApplicationId() {
        CreateNotificationRuleForm form = createForm(null, 1L, "rule-1", List.of(itemForm(10L, 20L, 0)));

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(comparisonGroup(1L)));
        when(notificationRuleRepository.existsByNotificationGroupIdAndName(1L, "rule-1")).thenReturn(false);
        when(applicationsRepository.findAllById(List.of(10L))).thenReturn(List.of(application(10L)));
        when(queryTemplateRepository.findAllById(List.of(20L))).thenReturn(List.of(queryTemplate(20L, application(99L))));

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_RULE_ERROR_APPLICATION_MISMATCH);
    }

    @Test
    void shouldThrowCheckTypeMismatchWhenGroupCheckTypeIsNotComparison() {
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        group.setCheckType(BaseConstant.NOTIFICATION_GROUP_CHECK_TYPE_THRESHOLD);

        CreateNotificationRuleForm form = createForm(null, 1L, "rule-1", List.of(itemForm(10L, 20L, 0)));

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_CHECK_TYPE_MISMATCH);
    }

    @Test
    void shouldDeleteNotificationRuleItemsBeforeRuleWhenDeletingRule() {
        NotificationRule entity = new NotificationRule();
        entity.setId(1L);

        when(notificationRuleRepository.findById(1L)).thenReturn(Optional.of(entity));

        ApiMessageDto<Void> result = controller.delete(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Delete notification rule success");
        InOrder inOrder = inOrder(notificationRuleItemRepository, notificationRuleRepository);
        inOrder.verify(notificationRuleItemRepository).deleteAllByNotificationRuleId(1L);
        inOrder.verify(notificationRuleRepository).delete(entity);
    }

    @Test
    void shouldReturnNotificationRuleDtoWithItemsFromItemRepositoryWhenGetIdExists() {
        NotificationRule entity = new NotificationRule();
        entity.setId(1L);
        NotificationRuleDto dto = new NotificationRuleDto();

        NotificationRuleItem itemEntity = new NotificationRuleItem();
        NotificationRuleItemDto itemDto = new NotificationRuleItemDto();

        when(notificationRuleRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationRuleMapper.fromEntityToNotificationRuleDto(entity)).thenReturn(dto);
        when(notificationRuleItemRepository.findAllByNotificationRuleIdOrderByOrdering(1L)).thenReturn(List.of(itemEntity));
        when(notificationRuleItemMapper.fromEntityToNotificationRuleItemDtoList(List.of(itemEntity))).thenReturn(List.of(itemDto));

        ApiMessageDto<NotificationRuleDto> result = controller.get(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(result.getData().getItems()).containsExactly(itemDto);
        assertThat(result.getMessage()).isEqualTo("Get notification rule success");
    }

    @Test
    void shouldThrowNotFoundWhenGetIdDoesNotExist() {
        when(notificationRuleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.get(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_RULE_ERROR_NOT_FOUND);
    }
}
