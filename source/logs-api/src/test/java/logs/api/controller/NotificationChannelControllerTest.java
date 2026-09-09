package logs.api.controller;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.ResponseListDto;
import logs.api.dto.notificationChannel.NotificationChannelDto;
import logs.api.exception.BadRequestException;
import logs.api.exception.NotFoundException;
import logs.api.form.notificationChannel.CreateNotificationChannelForm;
import logs.api.form.notificationChannel.UpdateNotificationChannelForm;
import logs.api.mapper.NotificationChannelMapper;
import logs.api.model.NotificationChannel;
import logs.api.model.criteria.NotificationChannelCriteria;
import logs.api.repository.NotificationChannelRepository;
import logs.api.repository.NotificationGroupRepository;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationChannelControllerTest {

    @Mock private NotificationChannelRepository notificationChannelRepository;
    @Mock private NotificationGroupRepository notificationGroupRepository;
    @Mock private NotificationChannelMapper notificationChannelMapper;
    @InjectMocks private NotificationChannelController controller;

    private final BindingResult bindingResult = mock(BindingResult.class);

    @Test
    void shouldThrowNotFoundWhenNotificationChannelIdDoesNotExistOnGet() {
        when(notificationChannelRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.get(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_CHANNEL_ERROR_NOT_FOUND);
    }

    @Test
    void shouldReturnNotificationChannelDtoWhenIdExists() {
        NotificationChannel entity = new NotificationChannel();
        NotificationChannelDto dto = new NotificationChannelDto();
        when(notificationChannelRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationChannelMapper.fromEntityToNotificationChannelDto(entity)).thenReturn(dto);

        ApiMessageDto<NotificationChannelDto> result = controller.get(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(result.getMessage()).isEqualTo("Get notification channel success");
    }

    @Test
    void shouldDelegateToRepositoryFindAllWhenListCalled() {
        NotificationChannelCriteria criteria = new NotificationChannelCriteria();
        Pageable pageable = mock(Pageable.class);
        Page<NotificationChannel> page = new PageImpl<>(Collections.singletonList(new NotificationChannel()));
        List<NotificationChannelDto> dtoList = Collections.singletonList(new NotificationChannelDto());
        when(notificationChannelRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(notificationChannelMapper.fromEntityToNotificationChannelDtoList(page.getContent())).thenReturn(dtoList);

        ApiMessageDto<ResponseListDto<List<NotificationChannelDto>>> result = controller.list(criteria, pageable);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData().getContent()).isEqualTo(dtoList);
        assertThat(result.getData().getTotalElements()).isEqualTo(page.getTotalElements());
        assertThat(result.getData().getTotalPages()).isEqualTo(page.getTotalPages());
        verify(notificationChannelRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldDefaultStatusActiveWhenAutoCompleteCalled() {
        NotificationChannelCriteria criteria = new NotificationChannelCriteria();
        Pageable pageable = mock(Pageable.class);
        Page<NotificationChannel> page = new PageImpl<>(Collections.singletonList(new NotificationChannel()));
        List<NotificationChannelDto> dtoList = Collections.singletonList(new NotificationChannelDto());
        when(notificationChannelRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(notificationChannelMapper.fromEntityListToNotificationChannelDtoAutoComplete(page.getContent())).thenReturn(dtoList);

        ApiMessageDto<ResponseListDto<List<NotificationChannelDto>>> result = controller.autoComplete(criteria, pageable);

        assertThat(criteria.getStatus()).isEqualTo(BaseConstant.STATUS_ACTIVE);
        assertThat(result.getResult()).isTrue();
        assertThat(result.getData().getContent()).isEqualTo(dtoList);
    }

    @Test
    void shouldCreateNotificationChannelWhenNameNotExisted() {
        CreateNotificationChannelForm form = new CreateNotificationChannelForm();
        form.setName("Slack");
        NotificationChannel entity = new NotificationChannel();
        NotificationChannelDto idDto = new NotificationChannelDto();
        when(notificationChannelRepository.existsByName("Slack")).thenReturn(false);
        when(notificationChannelMapper.fromFormToEntity(form)).thenReturn(entity);
        when(notificationChannelMapper.fromEntityToNotificationChannelIdDto(entity)).thenReturn(idDto);

        ApiMessageDto<NotificationChannelDto> result = controller.create(form, bindingResult);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(idDto);
        assertThat(result.getMessage()).isEqualTo("Create notification channel success");
        verify(notificationChannelRepository).save(entity);
    }

    @Test
    void shouldThrowBadRequestWhenNameExistedOnCreate() {
        CreateNotificationChannelForm form = new CreateNotificationChannelForm();
        form.setName("Slack");
        when(notificationChannelRepository.existsByName("Slack")).thenReturn(true);

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_CHANNEL_ERROR_NAME_EXISTED);
        verify(notificationChannelRepository, never()).save(any(NotificationChannel.class));
    }

    @Test
    void shouldThrowNotFoundWhenNotificationChannelIdDoesNotExistOnUpdate() {
        UpdateNotificationChannelForm form = new UpdateNotificationChannelForm();
        form.setId(1L);
        form.setName("Slack");
        when(notificationChannelRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_CHANNEL_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowBadRequestWhenNameExistedOnUpdate() {
        UpdateNotificationChannelForm form = new UpdateNotificationChannelForm();
        form.setId(1L);
        form.setName("Discord");
        NotificationChannel entity = new NotificationChannel();
        entity.setId(1L);
        entity.setName("Slack");
        when(notificationChannelRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationChannelRepository.existsByName("Discord")).thenReturn(true);

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_CHANNEL_ERROR_NAME_EXISTED);
        verify(notificationChannelRepository, never()).save(any(NotificationChannel.class));
    }

    @Test
    void shouldUpdateNotificationChannelWhenNameUnchanged() {
        UpdateNotificationChannelForm form = new UpdateNotificationChannelForm();
        form.setId(1L);
        form.setName("Slack");
        NotificationChannel entity = new NotificationChannel();
        entity.setId(1L);
        entity.setName("Slack");
        when(notificationChannelRepository.findById(1L)).thenReturn(Optional.of(entity));

        ApiMessageDto<Void> result = controller.update(form, bindingResult);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Update notification channel success");
        verify(notificationChannelRepository, never()).existsByName(any(String.class));
        verify(notificationChannelMapper).updateEntityFromForm(form, entity);
        verify(notificationChannelRepository).save(entity);
    }

    @Test
    void shouldThrowNotFoundWhenNotificationChannelIdDoesNotExistOnDelete() {
        when(notificationChannelRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.delete(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_CHANNEL_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowBadRequestWhenNotificationChannelInUseOnDelete() {
        NotificationChannel entity = new NotificationChannel();
        entity.setId(1L);
        when(notificationChannelRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationGroupRepository.existsByNotificationChannelId(1L)).thenReturn(true);

        assertThatThrownBy(() -> controller.delete(1L))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_CHANNEL_ERROR_IN_USE);
        verify(notificationChannelRepository, never()).delete(any(NotificationChannel.class));
    }

    @Test
    void shouldDeleteNotificationChannelWhenNotInUse() {
        NotificationChannel entity = new NotificationChannel();
        entity.setId(1L);
        when(notificationChannelRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationGroupRepository.existsByNotificationChannelId(1L)).thenReturn(false);

        ApiMessageDto<Void> result = controller.delete(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Delete notification channel success");
        verify(notificationChannelRepository, times(1)).delete(entity);
    }
}
