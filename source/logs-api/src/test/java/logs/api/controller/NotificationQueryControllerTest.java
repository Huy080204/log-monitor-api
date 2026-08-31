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
import logs.api.model.criteria.NotificationQueryCriteria;
import logs.api.repository.NotificationGroupRepository;
import logs.api.repository.NotificationQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationQueryControllerTest {

    @Mock
    private NotificationQueryRepository notificationQueryRepository;
    @Mock
    private NotificationGroupRepository notificationGroupRepository;
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
        form.setName("query name");
        form.setQuery("_time:5m error:true");
        form.setCount(10);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowBadRequestWhenCreateQueryExistedForGroup() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setName("query name");
        form.setQuery("_time:5m error:true");
        form.setCount(10);
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(notificationQueryRepository.existsByQueryAndNotificationGroupId("_time:5m error:true", 1L)).thenReturn(true);

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_QUERY_ERROR_EXISTED);
    }

    @Test
    void shouldThrowBadRequestWhenCreateNameExistedForGroup() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setName("query name");
        form.setQuery("_time:5m error:true");
        form.setCount(10);
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(notificationQueryRepository.existsByNameAndNotificationGroupId("query name", 1L)).thenReturn(true);

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_QUERY_ERROR_NAME_EXISTED);
    }

    @Test
    void shouldCreateAndAssignGroupWhenNoDuplicateExists() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setName("query name");
        form.setQuery("_time:5m error:true");
        form.setCount(10);
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        NotificationQuery entity = new NotificationQuery();
        NotificationQueryDto idDto = new NotificationQueryDto();
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(notificationQueryRepository.existsByQueryAndNotificationGroupId("_time:5m error:true", 1L)).thenReturn(false);
        when(notificationQueryRepository.existsByNameAndNotificationGroupId("query name", 1L)).thenReturn(false);
        when(notificationQueryMapper.fromFormToEntity(form)).thenReturn(entity);
        when(notificationQueryMapper.fromEntityToNotificationQueryIdDto(entity)).thenReturn(idDto);

        ApiMessageDto<NotificationQueryDto> result = controller.create(form, bindingResult);

        assertThat(entity.getNotificationGroup()).isSameAs(group);
        assertThat(result.getData()).isSameAs(idDto);
        assertThat(result.getMessage()).isEqualTo("Create notification query success");
        verify(notificationQueryRepository).save(entity);
    }

    @Test
    void shouldThrowNotFoundWhenUpdateNotificationQueryDoesNotExist() {
        UpdateNotificationQueryForm form = new UpdateNotificationQueryForm();
        form.setId(1L);
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_QUERY_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowBadRequestWhenUpdateQueryExistedForGroup() {
        UpdateNotificationQueryForm form = new UpdateNotificationQueryForm();
        form.setId(1L);
        form.setName("query name");
        form.setQuery("_time:5m error:true");
        form.setCount(10);
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        NotificationQuery entity = new NotificationQuery();
        entity.setId(1L);
        entity.setName("query name");
        entity.setQuery("_time:1m error:true");
        entity.setNotificationGroup(group);
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationQueryRepository.existsByQueryAndNotificationGroupId("_time:5m error:true", 1L)).thenReturn(true);

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_QUERY_ERROR_EXISTED);
    }

    @Test
    void shouldThrowBadRequestWhenUpdateNameExistedForGroup() {
        UpdateNotificationQueryForm form = new UpdateNotificationQueryForm();
        form.setId(1L);
        form.setName("new query name");
        form.setQuery("_time:5m error:true");
        form.setCount(10);
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        NotificationQuery entity = new NotificationQuery();
        entity.setId(1L);
        entity.setName("old query name");
        entity.setQuery("_time:5m error:true");
        entity.setNotificationGroup(group);
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationQueryRepository.existsByNameAndNotificationGroupId("new query name", 1L)).thenReturn(true);

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_QUERY_ERROR_NAME_EXISTED);
    }

    @Test
    void shouldSkipExistenceCheckWhenUpdateQueryUnchanged() {
        UpdateNotificationQueryForm form = new UpdateNotificationQueryForm();
        form.setId(1L);
        form.setName("query name");
        form.setQuery("_time:5m error:true");
        form.setCount(20);
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        NotificationQuery entity = new NotificationQuery();
        entity.setId(1L);
        entity.setName("query name");
        entity.setQuery("_time:5m error:true");
        entity.setNotificationGroup(group);
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.of(entity));

        ApiMessageDto<Void> result = controller.update(form, bindingResult);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Update notification query success");
        verify(notificationQueryRepository, never()).existsByQueryAndNotificationGroupId(any(), any());
        verify(notificationQueryRepository, never()).existsByNameAndNotificationGroupId(any(), any());
        verify(notificationQueryRepository).save(entity);
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
