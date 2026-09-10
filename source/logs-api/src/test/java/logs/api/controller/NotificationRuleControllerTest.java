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
import logs.api.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.validation.BindingResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    private static NotificationRuleItemForm itemForm(Long applicationId, Long queryTemplateId, Integer operator) {
        NotificationRuleItemForm form = new NotificationRuleItemForm();
        form.setApplicationId(applicationId);
        form.setQueryTemplateId(queryTemplateId);
        form.setOperator(operator);
        return form;
    }

    private static UpdateNotificationRuleItemForm updateItemForm(Long id, Long applicationId, Long queryTemplateId, Integer operator) {
        UpdateNotificationRuleItemForm form = new UpdateNotificationRuleItemForm();
        form.setId(id);
        form.setApplicationId(applicationId);
        form.setQueryTemplateId(queryTemplateId);
        form.setOperator(operator);
        return form;
    }

    private static CreateNotificationRuleForm createForm(Long notificationGroupId, String name, List<NotificationRuleItemForm> items) {
        CreateNotificationRuleForm form = new CreateNotificationRuleForm();
        form.setNotificationGroupId(notificationGroupId);
        form.setName(name);
        form.setItems(items);
        return form;
    }

    private static UpdateNotificationRuleForm updateForm(Long id, String name, List<UpdateNotificationRuleItemForm> items) {
        UpdateNotificationRuleForm form = new UpdateNotificationRuleForm();
        form.setId(id);
        form.setName(name);
        form.setItems(items);
        return form;
    }

    private static NotificationRuleItem existingItem(Long id) {
        NotificationRuleItem item = new NotificationRuleItem();
        item.setId(id);
        return item;
    }

    // ---------- get ----------

    @Test
    void shouldThrowNotFoundWhenGetIdDoesNotExist() {
        when(notificationRuleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.get(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_RULE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldReturnNotificationRuleDtoWithItemsWhenGetIdExists() {
        NotificationRule entity = new NotificationRule();
        entity.setId(1L);
        NotificationRuleDto dto = new NotificationRuleDto();
        NotificationRuleItem itemEntity = existingItem(401L);
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

    // ---------- list ----------

    @Test
    void shouldReturnPagedListWhenListCalled() {
        NotificationRuleCriteria criteria = new NotificationRuleCriteria();
        Pageable pageable = PageRequest.of(0, 10);
        NotificationRule entity = new NotificationRule();
        NotificationRuleDto dto = new NotificationRuleDto();

        when(notificationRuleRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(notificationRuleMapper.fromEntityToNotificationRuleDtoList(List.of(entity))).thenReturn(List.of(dto));

        ApiMessageDto<ResponseListDto<List<NotificationRuleDto>>> result = controller.list(criteria, pageable);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData().getContent()).containsExactly(dto);
        assertThat(result.getData().getTotalElements()).isEqualTo(1);
        assertThat(result.getMessage()).isEqualTo("Get list success");
    }

    // ---------- create ----------

    @Test
    void shouldThrowGroupNotFoundWhenCreatingWithUnknownGroupId() {
        CreateNotificationRuleForm form = createForm(1L, "rule-1", List.of(itemForm(10L, 20L, null)));

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowCheckTypeMismatchWhenGroupCheckTypeIsNotComparison() {
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        group.setCheckType(BaseConstant.NOTIFICATION_GROUP_CHECK_TYPE_THRESHOLD);

        CreateNotificationRuleForm form = createForm(1L, "rule-1", List.of(itemForm(10L, 20L, null)));

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_CHECK_TYPE_MISMATCH);
    }

    @Test
    void shouldThrowNameExistedWhenCreatingDuplicateNameInGroup() {
        CreateNotificationRuleForm form = createForm(1L, "dup", List.of(itemForm(10L, 20L, null)));

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(comparisonGroup(1L)));
        when(notificationRuleRepository.existsByNotificationGroupIdAndName(1L, "dup")).thenReturn(true);

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_RULE_ERROR_NAME_EXISTED);
    }

    @Test
    void shouldThrowApplicationNotFoundWhenItemApplicationIdDoesNotExist() {
        CreateNotificationRuleForm form = createForm(1L, "rule-1", List.of(itemForm(10L, 20L, null)));

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(comparisonGroup(1L)));
        when(notificationRuleRepository.existsByNotificationGroupIdAndName(1L, "rule-1")).thenReturn(false);
        when(applicationsRepository.findAllById(anyList())).thenReturn(List.of());

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.APPLICATIONS_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowQueryTemplateNotFoundWhenItemQueryTemplateIdDoesNotExist() {
        CreateNotificationRuleForm form = createForm(1L, "rule-1", List.of(itemForm(10L, 20L, null)));

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(comparisonGroup(1L)));
        when(notificationRuleRepository.existsByNotificationGroupIdAndName(1L, "rule-1")).thenReturn(false);
        when(applicationsRepository.findAllById(anyList())).thenReturn(List.of(application(10L)));
        when(queryTemplateRepository.findAllById(anyList())).thenReturn(List.of());

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowApplicationMismatchWhenQueryTemplateApplicationDiffersFromItemApplicationId() {
        CreateNotificationRuleForm form = createForm(1L, "rule-1", List.of(itemForm(10L, 20L, null)));

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(comparisonGroup(1L)));
        when(notificationRuleRepository.existsByNotificationGroupIdAndName(1L, "rule-1")).thenReturn(false);
        when(applicationsRepository.findAllById(anyList())).thenReturn(List.of(application(10L)));
        when(queryTemplateRepository.findAllById(anyList())).thenReturn(List.of(queryTemplate(20L, application(99L))));

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_CHECK_TYPE_MISMATCH);
    }

    @Test
    void shouldCreateRuleAndItemsForcingNullOperatorAtFirstItemWhenValid() {
        NotificationGroup group = comparisonGroup(1L);
        CreateNotificationRuleForm form = createForm(1L, "rule-1",
                List.of(itemForm(10L, 20L, 5), itemForm(11L, 21L, 0)));

        NotificationRule newRule = new NotificationRule();
        NotificationRuleDto idDto = new NotificationRuleDto();

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(notificationRuleRepository.existsByNotificationGroupIdAndName(1L, "rule-1")).thenReturn(false);
        when(applicationsRepository.findAllById(anyList()))
                .thenReturn(List.of(application(10L), application(11L)));
        when(queryTemplateRepository.findAllById(anyList()))
                .thenReturn(List.of(queryTemplate(20L, application(10L)), queryTemplate(21L, application(11L))));
        when(notificationRuleMapper.fromFormToEntity(form)).thenReturn(newRule);
        when(notificationRuleMapper.fromEntityToNotificationRuleIdDto(newRule)).thenReturn(idDto);

        ArgumentCaptor<List<NotificationRuleItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);

        ApiMessageDto<NotificationRuleDto> result = controller.create(form, bindingResult);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(idDto);
        assertThat(result.getMessage()).isEqualTo("Create notification rule success");
        assertThat(newRule.getNotificationGroup()).isSameAs(group);
        verify(notificationRuleRepository).save(newRule);
        verify(notificationRuleItemRepository).saveAll(itemsCaptor.capture());

        List<NotificationRuleItem> savedItems = itemsCaptor.getValue();
        assertThat(savedItems).hasSize(2);
        assertThat(savedItems.get(0).getOrdering()).isEqualTo(0);
        assertThat(savedItems.get(0).getOperator()).isNull();
        assertThat(savedItems.get(0).getNotificationRule()).isSameAs(newRule);
        assertThat(savedItems.get(1).getOrdering()).isEqualTo(1);
        assertThat(savedItems.get(1).getOperator()).isEqualTo(0);
        assertThat(savedItems.get(1).getNotificationRule()).isSameAs(newRule);
    }

    // ---------- update ----------

    @Test
    void shouldThrowNotFoundWhenUpdatingUnknownRuleId() {
        UpdateNotificationRuleForm form = updateForm(10L, "rule-10", List.of(updateItemForm(null, 10L, 20L, null)));

        when(notificationRuleRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_RULE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowNameExistedWhenUpdatingToDuplicateNameExcludingSelf() {
        NotificationRule existingRule = new NotificationRule();
        existingRule.setId(10L);
        existingRule.setName("old-name");
        existingRule.setNotificationGroup(comparisonGroup(1L));

        UpdateNotificationRuleForm form = updateForm(10L, "dup", List.of(updateItemForm(null, 10L, 20L, null)));

        when(notificationRuleRepository.findById(10L)).thenReturn(Optional.of(existingRule));
        when(notificationRuleRepository.existsByNotificationGroupIdAndNameAndIdNot(1L, "dup", 10L)).thenReturn(true);

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_RULE_ERROR_NAME_EXISTED);
    }

    @Test
    void shouldThrowItemNotFoundWhenUpdateItemIdDoesNotBelongToRule() {
        NotificationRule existingRule = new NotificationRule();
        existingRule.setId(10L);
        existingRule.setName("rule-10");
        existingRule.setNotificationGroup(comparisonGroup(1L));

        UpdateNotificationRuleForm form = updateForm(10L, "rule-10",
                List.of(updateItemForm(999L, 10L, 20L, null)));

        when(notificationRuleRepository.findById(10L)).thenReturn(Optional.of(existingRule));
        when(applicationsRepository.findAllById(anyList())).thenReturn(List.of(application(10L)));
        when(queryTemplateRepository.findAllById(anyList())).thenReturn(List.of(queryTemplate(20L, application(10L))));
        when(notificationRuleItemRepository.findAllByNotificationRuleIdOrderByOrdering(10L)).thenReturn(List.of());

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_RULE_ERROR_ITEM_NOT_FOUND);
    }

    @Test
    void shouldUpdateRuleAndMergeItemsByIdWhenIdsProvided() {
        NotificationRule existingRule = new NotificationRule();
        existingRule.setId(10L);
        existingRule.setName("old-name");
        existingRule.setNotificationGroup(comparisonGroup(1L));

        NotificationRuleItem keptItem = existingItem(401L);
        NotificationRuleItem leftoverItem = existingItem(402L);

        UpdateNotificationRuleForm form = updateForm(10L, "new-name", List.of(
                updateItemForm(401L, 10L, 20L, 5),
                updateItemForm(null, 11L, 21L, 0)
        ));

        when(notificationRuleRepository.findById(10L)).thenReturn(Optional.of(existingRule));
        when(notificationRuleRepository.existsByNotificationGroupIdAndNameAndIdNot(1L, "new-name", 10L)).thenReturn(false);
        when(applicationsRepository.findAllById(anyList()))
                .thenReturn(List.of(application(10L), application(11L)));
        when(queryTemplateRepository.findAllById(anyList()))
                .thenReturn(List.of(queryTemplate(20L, application(10L)), queryTemplate(21L, application(11L))));
        when(notificationRuleItemRepository.findAllByNotificationRuleIdOrderByOrdering(10L))
                .thenReturn(List.of(keptItem, leftoverItem));
        doAnswer(invocation -> {
            UpdateNotificationRuleForm f = invocation.getArgument(0);
            NotificationRule e = invocation.getArgument(1);
            e.setName(f.getName());
            return null;
        }).when(notificationRuleMapper).updateEntityFromForm(any(UpdateNotificationRuleForm.class), any(NotificationRule.class));

        ArgumentCaptor<List<NotificationRuleItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> deletedIdsCaptor = ArgumentCaptor.forClass(Collection.class);

        ApiMessageDto<Void> result = controller.update(form, bindingResult);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Update notification rule success");
        assertThat(existingRule.getName()).isEqualTo("new-name");
        verify(notificationRuleRepository).save(existingRule);

        verify(notificationRuleItemRepository).deleteAllByIdIn(deletedIdsCaptor.capture());
        assertThat(deletedIdsCaptor.getValue()).containsExactly(402L);

        verify(notificationRuleItemRepository).saveAll(itemsCaptor.capture());
        List<NotificationRuleItem> savedItems = itemsCaptor.getValue();
        assertThat(savedItems).hasSize(2);
        assertThat(savedItems.get(0)).isSameAs(keptItem);
        assertThat(savedItems.get(0).getOrdering()).isEqualTo(0);
        assertThat(savedItems.get(0).getOperator()).isNull();
        assertThat(savedItems.get(1).getOrdering()).isEqualTo(1);
        assertThat(savedItems.get(1).getOperator()).isEqualTo(0);
        assertThat(savedItems.get(1).getNotificationRule()).isSameAs(existingRule);
    }

    @Test
    void shouldNotDeleteLeftoverItemsWhenAllExistingItemsAreKept() {
        NotificationRule existingRule = new NotificationRule();
        existingRule.setId(10L);
        existingRule.setName("rule-10");
        existingRule.setNotificationGroup(comparisonGroup(1L));

        NotificationRuleItem keptItem = existingItem(401L);

        UpdateNotificationRuleForm form = updateForm(10L, "rule-10", List.of(
                updateItemForm(401L, 10L, 20L, null),
                updateItemForm(null, 11L, 21L, 0)
        ));

        when(notificationRuleRepository.findById(10L)).thenReturn(Optional.of(existingRule));
        when(applicationsRepository.findAllById(anyList()))
                .thenReturn(List.of(application(10L), application(11L)));
        when(queryTemplateRepository.findAllById(anyList()))
                .thenReturn(List.of(queryTemplate(20L, application(10L)), queryTemplate(21L, application(11L))));
        when(notificationRuleItemRepository.findAllByNotificationRuleIdOrderByOrdering(10L))
                .thenReturn(List.of(keptItem));

        controller.update(form, bindingResult);

        verify(notificationRuleItemRepository, never()).deleteAllByIdIn(any());
        verify(notificationRuleItemRepository).saveAll(anyList());
    }

    // ---------- delete ----------

    @Test
    void shouldThrowNotFoundWhenDeletingUnknownRuleId() {
        when(notificationRuleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.delete(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_RULE_ERROR_NOT_FOUND);
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
}
