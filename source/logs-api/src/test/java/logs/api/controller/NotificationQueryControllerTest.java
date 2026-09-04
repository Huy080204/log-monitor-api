package logs.api.controller;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.ResponseListDto;
import logs.api.dto.notificationQuery.NotificationQueryDto;
import logs.api.exception.BadRequestException;
import logs.api.exception.NotFoundException;
import logs.api.form.notificationQuery.CreateNotificationQueryForm;
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        verify(notificationQueryRepository, never()).save(any());
        verify(notificationQueryRepository, never()).deleteAllByNotificationGroupId(any());
        verify(notificationQueryRepository, never()).deleteAllByNotificationGroupIdAndQueryTemplateIdNotIn(any(), any());
    }

    @Test
    void shouldThrowNotFoundWhenCreateQueryTemplateIdDoesNotExist() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setTemplateQueryIds(List.of(2L, 3L));
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate t2 = new QueryTemplate();
        t2.setId(2L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findAllById(any())).thenReturn(List.of(t2));

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND);

        verify(notificationQueryRepository, never()).save(any());
        verify(notificationQueryRepository, never()).deleteAllByNotificationGroupId(any());
        verify(notificationQueryRepository, never()).deleteAllByNotificationGroupIdAndQueryTemplateIdNotIn(any(), any());
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
                .hasMessage("Duplicate query template in request");

        verify(notificationQueryRepository, never()).save(any());
        verify(notificationQueryRepository, never()).deleteAllByNotificationGroupId(any());
        verify(notificationQueryRepository, never()).deleteAllByNotificationGroupIdAndQueryTemplateIdNotIn(any(), any());
    }

    @Test
    void shouldSaveBothRowsWhenGroupHasNoExistingQueries() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setTemplateQueryIds(List.of(10L, 20L));

        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate t10 = new QueryTemplate();
        t10.setId(10L);
        QueryTemplate t20 = new QueryTemplate();
        t20.setId(20L);

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findAllById(any())).thenReturn(List.of(t10, t20));
        when(notificationQueryRepository.findAllByNotificationGroupId(1L)).thenReturn(List.of());

        ApiMessageDto<Void> result = controller.create(form, bindingResult);

        verify(notificationQueryRepository, times(2)).save(any(NotificationQuery.class));
        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isNull();
        assertThat(result.getMessage()).isEqualTo("Create notification query success");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSyncQueriesWhenGroupAlreadyLinkedToOtherTemplates() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setTemplateQueryIds(List.of(2L, 3L, 4L));

        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate t2 = new QueryTemplate();
        t2.setId(2L);
        QueryTemplate t3 = new QueryTemplate();
        t3.setId(3L);
        QueryTemplate t4 = new QueryTemplate();
        t4.setId(4L);

        // t1's row is already gone from this read-back — it is the bulk-delete's job to have
        // removed it; this mock represents the post-delete state, not a pre-delete snapshot.
        NotificationQuery row2 = new NotificationQuery();
        row2.setId(102L);
        row2.setNotificationGroup(group);
        row2.setQueryTemplate(t2);
        row2.setStatus(BaseConstant.STATUS_PENDING);

        NotificationQuery row3 = new NotificationQuery();
        row3.setId(103L);
        row3.setNotificationGroup(group);
        row3.setQueryTemplate(t3);

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findAllById(any())).thenReturn(List.of(t2, t3, t4));
        when(notificationQueryRepository.findAllByNotificationGroupId(1L)).thenReturn(List.of(row2, row3));

        ArgumentCaptor<Collection<Long>> notInIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<NotificationQuery> saveCaptor = ArgumentCaptor.forClass(NotificationQuery.class);

        controller.create(form, bindingResult);

        verify(notificationQueryRepository).deleteAllByNotificationGroupIdAndQueryTemplateIdNotIn(eq(1L), notInIdsCaptor.capture());
        assertThat(notInIdsCaptor.getValue()).containsExactlyInAnyOrder(2L, 3L, 4L);
        verify(notificationQueryRepository, times(1)).save(saveCaptor.capture());
        assertThat(saveCaptor.getValue().getQueryTemplate()).isSameAs(t4);
        assertThat(saveCaptor.getValue().getNotificationGroup()).isSameAs(group);
        verify(notificationQueryRepository, never()).save(same(row2));
        verify(notificationQueryRepository, never()).save(same(row3));
        // status untouched — sync never re-saves a retained row, so PENDING must not be reset.
        assertThat(row2.getStatus()).isEqualTo(BaseConstant.STATUS_PENDING);
    }

    @Test
    void shouldDeleteAllWhenTemplateQueryIdsEmpty() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setTemplateQueryIds(List.of());

        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        ApiMessageDto<Void> result = controller.create(form, bindingResult);

        verify(notificationQueryRepository).deleteAllByNotificationGroupId(1L);
        verify(notificationQueryRepository, never()).deleteAllByNotificationGroupIdAndQueryTemplateIdNotIn(any(), any());
        verify(notificationQueryRepository, never()).save(any());
        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isNull();
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
