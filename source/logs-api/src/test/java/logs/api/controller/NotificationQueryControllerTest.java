package logs.api.controller;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.ResponseListDto;
import logs.api.dto.notificationQuery.NotificationQueryDto;
import logs.api.exception.BadRequestException;
import logs.api.exception.NotFoundException;
import logs.api.form.notificationQuery.CreateNotificationQueryForm;
import logs.api.form.notificationQuery.ReplaceNotificationQueryForm;
import logs.api.mapper.NotificationQueryMapper;
import logs.api.model.NotificationGroup;
import logs.api.model.NotificationQuery;
import logs.api.model.QueryTemplate;
import logs.api.model.criteria.NotificationQueryCriteria;
import logs.api.repository.NotificationGroupRepository;
import logs.api.repository.NotificationQueryRepository;
import logs.api.repository.QueryTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationQueryControllerTest {

    @Mock
    private NotificationQueryRepository notificationQueryRepository;
    @Mock
    private NotificationGroupRepository notificationGroupRepository;
    @Mock
    private QueryTemplateRepository queryTemplateRepository;
    @Mock
    private NotificationQueryMapper notificationQueryMapper;
    @InjectMocks
    private NotificationQueryController controller;

    private final BindingResult bindingResult = mock(BindingResult.class);

    @Test
    void shouldThrowNotFoundWhenGetIdDoesNotExist() {
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.get(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_QUERY_ERROR_NOT_FOUND);
    }

    @Test
    void shouldReturnNotificationQueryDtoWhenGetIdExists() {
        NotificationQuery entity = new NotificationQuery();
        NotificationQueryDto dto = new NotificationQueryDto();
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationQueryMapper.fromEntityToNotificationQueryDto(entity)).thenReturn(dto);

        ApiMessageDto<NotificationQueryDto> result = controller.get(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(result.getMessage()).isEqualTo("Get notification query success");
    }

    @Test
    void shouldReturnPagedListWhenListCalled() {
        NotificationQuery entity = new NotificationQuery();
        NotificationQueryDto dto = new NotificationQueryDto();
        Page<NotificationQuery> page = new PageImpl<>(List.of(entity));
        when(notificationQueryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(notificationQueryMapper.fromEntityListToNotificationQueryDtoList(List.of(entity))).thenReturn(List.of(dto));

        ApiMessageDto<ResponseListDto<List<NotificationQueryDto>>> result =
                controller.list(new NotificationQueryCriteria(), Pageable.unpaged());

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData().getContent()).containsExactly(dto);
    }

    @Test
    void shouldThrowNotFoundWhenCreateNotificationGroupDoesNotExist() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setTemplateQueryIds(List.of(2L));
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowNotFoundWhenCreateQueryTemplateIdInListDoesNotExist() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setTemplateQueryIds(List.of(2L, 3L));
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate t2 = new QueryTemplate();
        t2.setId(2L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findById(2L)).thenReturn(Optional.of(t2));
        when(queryTemplateRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowBadRequestWhenCreateTemplateQueryIdsHasDuplicate() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setTemplateQueryIds(List.of(5L, 5L));
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_QUERY_ERROR_DUPLICATED);
    }

    @Test
    void shouldThrowBadRequestWhenCreateNotificationGroupAlreadyConfigured() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setTemplateQueryIds(List.of(2L));
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(notificationQueryRepository.existsByNotificationGroupId(1L)).thenReturn(true);

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_QUERY_ERROR_EXISTED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateBulkWhenNotificationGroupNotYetConfigured() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setTemplateQueryIds(List.of(5L, 6L));
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate t5 = new QueryTemplate();
        t5.setId(5L);
        QueryTemplate t6 = new QueryTemplate();
        t6.setId(6L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(notificationQueryRepository.existsByNotificationGroupId(1L)).thenReturn(false);
        when(queryTemplateRepository.findById(5L)).thenReturn(Optional.of(t5));
        when(queryTemplateRepository.findById(6L)).thenReturn(Optional.of(t6));

        ArgumentCaptor<List<NotificationQuery>> saveAllCaptor = ArgumentCaptor.forClass(List.class);

        ApiMessageDto<Void> result = controller.create(form, bindingResult);

        verify(notificationQueryRepository).saveAll(saveAllCaptor.capture());
        assertThat(saveAllCaptor.getValue()).hasSize(2);
        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isNull();
        assertThat(result.getMessage()).isEqualTo("Create notification query success");
    }

    @Test
    void shouldThrowNotFoundWhenReplaceNotificationGroupDoesNotExist() {
        ReplaceNotificationQueryForm form = new ReplaceNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setTemplateQueryIds(List.of(2L));
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.replace(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND);

        verify(notificationQueryRepository, never()).deleteAll(any());
        verify(notificationQueryRepository, never()).save(any());
    }

    @Test
    void shouldThrowNotFoundWhenReplaceQueryTemplateIdDoesNotExist() {
        ReplaceNotificationQueryForm form = new ReplaceNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setTemplateQueryIds(List.of(2L, 3L));
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate t2 = new QueryTemplate();
        t2.setId(2L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findById(2L)).thenReturn(Optional.of(t2));
        when(queryTemplateRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.replace(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowBadRequestWhenReplaceTemplateQueryIdsHasDuplicate() {
        ReplaceNotificationQueryForm form = new ReplaceNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setTemplateQueryIds(List.of(5L, 5L));
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> controller.replace(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_QUERY_ERROR_DUPLICATED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReplaceQueriesWhenNotificationGroupExists() {
        ReplaceNotificationQueryForm form = new ReplaceNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setTemplateQueryIds(List.of(2L, 3L, 4L));

        NotificationGroup group = new NotificationGroup();
        group.setId(1L);

        QueryTemplate t1 = new QueryTemplate();
        t1.setId(1L);
        QueryTemplate t2 = new QueryTemplate();
        t2.setId(2L);
        QueryTemplate t3 = new QueryTemplate();
        t3.setId(3L);
        QueryTemplate t4 = new QueryTemplate();
        t4.setId(4L);

        NotificationQuery row1 = new NotificationQuery();
        row1.setId(101L);
        row1.setNotificationGroup(group);
        row1.setQueryTemplate(t1);

        NotificationQuery row2 = new NotificationQuery();
        row2.setId(102L);
        row2.setNotificationGroup(group);
        row2.setQueryTemplate(t2);

        NotificationQuery row3 = new NotificationQuery();
        row3.setId(103L);
        row3.setNotificationGroup(group);
        row3.setQueryTemplate(t3);

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findById(2L)).thenReturn(Optional.of(t2));
        when(queryTemplateRepository.findById(3L)).thenReturn(Optional.of(t3));
        when(queryTemplateRepository.findById(4L)).thenReturn(Optional.of(t4));
        when(notificationQueryRepository.findAllByNotificationGroupId(1L)).thenReturn(List.of(row1, row2, row3));

        ArgumentCaptor<List<NotificationQuery>> deleteCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<NotificationQuery> saveCaptor = ArgumentCaptor.forClass(NotificationQuery.class);

        ApiMessageDto<Void> result = controller.replace(form, bindingResult);

        verify(notificationQueryRepository).deleteAll(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue()).containsExactly(row1);
        verify(notificationQueryRepository, times(1)).save(saveCaptor.capture());
        assertThat(saveCaptor.getValue().getQueryTemplate()).isSameAs(t4);
        assertThat(saveCaptor.getValue().getNotificationGroup()).isSameAs(group);
        verify(notificationQueryRepository, never()).save(same(row2));
        verify(notificationQueryRepository, never()).save(same(row3));
        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isNull();
        assertThat(result.getMessage()).isEqualTo("Replace notification query success");
    }

    @Test
    void shouldKeepExistingStatusWhenReplaceRetainsLinkedQuery() {
        ReplaceNotificationQueryForm form = new ReplaceNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setTemplateQueryIds(List.of(2L));

        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate t2 = new QueryTemplate();
        t2.setId(2L);

        NotificationQuery row2 = new NotificationQuery();
        row2.setId(102L);
        row2.setNotificationGroup(group);
        row2.setQueryTemplate(t2);
        row2.setStatus(BaseConstant.STATUS_PENDING);

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findById(2L)).thenReturn(Optional.of(t2));
        when(notificationQueryRepository.findAllByNotificationGroupId(1L)).thenReturn(List.of(row2));

        controller.replace(form, bindingResult);

        assertThat(row2.getStatus()).isEqualTo(BaseConstant.STATUS_PENDING);
        verify(notificationQueryRepository, never()).save(same(row2));
        verify(notificationQueryRepository, never()).deleteAll(any());
    }

    @Test
    void shouldThrowNotFoundWhenDeleteIdDoesNotExist() {
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.delete(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_QUERY_ERROR_NOT_FOUND);
    }

    @Test
    void shouldDeleteWhenIdExists() {
        NotificationQuery entity = new NotificationQuery();
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.of(entity));

        ApiMessageDto<Void> result = controller.delete(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Delete notification query success");
        verify(notificationQueryRepository).delete(entity);
    }
}
