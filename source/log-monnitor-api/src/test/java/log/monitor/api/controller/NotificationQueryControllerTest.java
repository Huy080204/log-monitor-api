package log.monitor.api.controller;

import log.monitor.api.dto.ApiMessageDto;
import log.monitor.api.dto.ErrorCode;
import log.monitor.api.dto.ResponseListDto;
import log.monitor.api.dto.notificationQuery.NotificationQueryDto;
import log.monitor.api.exception.BadRequestException;
import log.monitor.api.exception.NotFoundException;
import log.monitor.api.form.notificationQuery.CreateNotificationQueryForm;
import log.monitor.api.form.notificationQuery.UpdateNotificationQueryForm;
import log.monitor.api.mapper.NotificationQueryMapper;
import log.monitor.api.model.NotificationGroup;
import log.monitor.api.model.NotificationQuery;
import log.monitor.api.model.criteria.NotificationQueryCriteria;
import log.monitor.api.repository.NotificationGroupRepository;
import log.monitor.api.repository.NotificationQueryRepository;
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
        form.setQuery("_time:5m error:true");
        form.setCount(10);
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(notificationQueryRepository.existsByQueryAndNotificationGroupIdCaseSensitive("_time:5m error:true", 1L)).thenReturn(true);

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_QUERY_ERROR_EXISTED);
    }

    @Test
    void shouldCreateAndAssignGroupWhenNoDuplicateExists() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setQuery("_time:5m error:true");
        form.setCount(10);
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        NotificationQuery entity = new NotificationQuery();
        NotificationQueryDto idDto = new NotificationQueryDto();
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(notificationQueryRepository.existsByQueryAndNotificationGroupIdCaseSensitive("_time:5m error:true", 1L)).thenReturn(false);
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
    void shouldThrowNotFoundWhenUpdateNotificationGroupDoesNotExist() {
        UpdateNotificationQueryForm form = new UpdateNotificationQueryForm();
        form.setId(1L);
        form.setNotificationGroupId(2L);
        form.setQuery("_time:5m error:true");
        NotificationQuery entity = new NotificationQuery();
        entity.setId(1L);
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationGroupRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowBadRequestWhenUpdateQueryExistedForChangedGroup() {
        UpdateNotificationQueryForm form = new UpdateNotificationQueryForm();
        form.setId(1L);
        form.setNotificationGroupId(2L);
        form.setQuery("_time:5m error:true");
        form.setCount(10);
        NotificationGroup oldGroup = new NotificationGroup();
        oldGroup.setId(1L);
        NotificationQuery entity = new NotificationQuery();
        entity.setId(1L);
        entity.setQuery("_time:5m error:true");
        entity.setNotificationGroup(oldGroup);
        NotificationGroup newGroup = new NotificationGroup();
        newGroup.setId(2L);
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationGroupRepository.findById(2L)).thenReturn(Optional.of(newGroup));
        when(notificationQueryRepository.existsByQueryAndNotificationGroupIdCaseSensitive("_time:5m error:true", 2L)).thenReturn(true);

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_QUERY_ERROR_EXISTED);
    }

    @Test
    void shouldSkipExistenceCheckWhenUpdateQueryAndGroupUnchanged() {
        UpdateNotificationQueryForm form = new UpdateNotificationQueryForm();
        form.setId(1L);
        form.setNotificationGroupId(1L);
        form.setQuery("_time:5m error:true");
        form.setCount(20);
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        NotificationQuery entity = new NotificationQuery();
        entity.setId(1L);
        entity.setQuery("_time:5m error:true");
        entity.setNotificationGroup(group);
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        ApiMessageDto<Void> result = controller.update(form, bindingResult);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Update notification query success");
        assertThat(entity.getNotificationGroup()).isSameAs(group);
        verify(notificationQueryRepository, never()).existsByQueryAndNotificationGroupIdCaseSensitive(any(), any());
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
