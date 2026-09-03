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
        form.setQueryTemplateId(2L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowNotFoundWhenCreateQueryTemplateDoesNotExist() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setQueryTemplateId(2L);
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowBadRequestWhenCreateDuplicateLinkExists() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setQueryTemplateId(2L);
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate queryTemplate = new QueryTemplate();
        queryTemplate.setId(2L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findById(2L)).thenReturn(Optional.of(queryTemplate));
        when(notificationQueryRepository.existsByNotificationGroupIdAndQueryTemplateId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_QUERY_ERROR_EXISTED);
    }

    @Test
    void shouldCreateWhenDuplicateLinkDoesNotExist() {
        CreateNotificationQueryForm form = new CreateNotificationQueryForm();
        form.setNotificationGroupId(1L);
        form.setQueryTemplateId(2L);
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate queryTemplate = new QueryTemplate();
        queryTemplate.setId(2L);
        NotificationQueryDto idDto = new NotificationQueryDto();
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(queryTemplateRepository.findById(2L)).thenReturn(Optional.of(queryTemplate));
        when(notificationQueryRepository.existsByNotificationGroupIdAndQueryTemplateId(1L, 2L)).thenReturn(false);
        when(notificationQueryMapper.fromEntityToNotificationQueryIdDto(any(NotificationQuery.class))).thenReturn(idDto);
        ArgumentCaptor<NotificationQuery> captor = ArgumentCaptor.forClass(NotificationQuery.class);

        ApiMessageDto<NotificationQueryDto> result = controller.create(form, bindingResult);

        verify(notificationQueryRepository).save(captor.capture());
        assertThat(captor.getValue().getNotificationGroup()).isSameAs(group);
        assertThat(captor.getValue().getQueryTemplate()).isSameAs(queryTemplate);
        assertThat(result.getData()).isSameAs(idDto);
        assertThat(result.getMessage()).isEqualTo("Create notification query success");
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
    void shouldThrowNotFoundWhenUpdateQueryTemplateDoesNotExist() {
        UpdateNotificationQueryForm form = new UpdateNotificationQueryForm();
        form.setId(1L);
        form.setQueryTemplateId(3L);
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate oldTemplate = new QueryTemplate();
        oldTemplate.setId(2L);
        NotificationQuery entity = new NotificationQuery();
        entity.setId(1L);
        entity.setNotificationGroup(group);
        entity.setQueryTemplate(oldTemplate);
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(queryTemplateRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowBadRequestWhenUpdateQueryTemplateChangedAndDuplicateLinkExists() {
        UpdateNotificationQueryForm form = new UpdateNotificationQueryForm();
        form.setId(1L);
        form.setQueryTemplateId(3L);
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate oldTemplate = new QueryTemplate();
        oldTemplate.setId(2L);
        QueryTemplate newTemplate = new QueryTemplate();
        newTemplate.setId(3L);
        NotificationQuery entity = new NotificationQuery();
        entity.setId(1L);
        entity.setNotificationGroup(group);
        entity.setQueryTemplate(oldTemplate);
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(queryTemplateRepository.findById(3L)).thenReturn(Optional.of(newTemplate));
        when(notificationQueryRepository.existsByNotificationGroupIdAndQueryTemplateId(1L, 3L)).thenReturn(true);

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_QUERY_ERROR_EXISTED);
    }

    @Test
    void shouldSkipDuplicateLinkCheckWhenUpdateQueryTemplateUnchanged() {
        UpdateNotificationQueryForm form = new UpdateNotificationQueryForm();
        form.setId(1L);
        form.setQueryTemplateId(2L);
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        QueryTemplate queryTemplate = new QueryTemplate();
        queryTemplate.setId(2L);
        NotificationQuery entity = new NotificationQuery();
        entity.setId(1L);
        entity.setNotificationGroup(group);
        entity.setQueryTemplate(queryTemplate);
        when(notificationQueryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(queryTemplateRepository.findById(2L)).thenReturn(Optional.of(queryTemplate));

        ApiMessageDto<Void> result = controller.update(form, bindingResult);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Update notification query success");
        verify(notificationQueryRepository, never()).existsByNotificationGroupIdAndQueryTemplateId(any(), any());
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
