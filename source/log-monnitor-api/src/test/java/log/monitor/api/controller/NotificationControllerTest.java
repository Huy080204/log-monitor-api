package log.monitor.api.controller;

import log.monitor.api.constant.BaseConstant;
import log.monitor.api.dto.ApiMessageDto;
import log.monitor.api.dto.ErrorCode;
import log.monitor.api.dto.ResponseListDto;
import log.monitor.api.dto.notification.NotificationDto;
import log.monitor.api.exception.NotFoundException;
import log.monitor.api.form.notification.ReadNotificationForm;
import log.monitor.api.mapper.NotificationMapper;
import log.monitor.api.model.Notification;
import log.monitor.api.model.criteria.NotificationCriteria;
import log.monitor.api.repository.NotificationRepository;
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
class NotificationControllerTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @InjectMocks
    private NotificationController controller;

    private final BindingResult bindingResult = mock(BindingResult.class);

    @Test
    void shouldThrowNotFoundWhenGetIdDoesNotExist() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.get(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_ERROR_NOT_FOUND);
    }

    @Test
    void shouldReturnNotificationDtoWhenGetIdExists() {
        Notification entity = new Notification();
        NotificationDto dto = new NotificationDto();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationMapper.fromEntityToNotificationDto(entity)).thenReturn(dto);

        ApiMessageDto<NotificationDto> result = controller.get(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(result.getMessage()).isEqualTo("Get notification success");
    }

    @Test
    void shouldReturnPagedListWhenListCalled() {
        Notification entity = new Notification();
        NotificationDto dto = new NotificationDto();
        Page<Notification> page = new PageImpl<>(List.of(entity));
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(notificationMapper.fromEntityListToNotificationDtoList(List.of(entity))).thenReturn(List.of(dto));

        ApiMessageDto<ResponseListDto<List<NotificationDto>>> result =
                controller.list(new NotificationCriteria(), Pageable.unpaged());

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData().getContent()).containsExactly(dto);
        assertThat(result.getData().getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldReturnPagedListWhenAutoCompleteCalled() {
        Notification entity = new Notification();
        NotificationDto dto = new NotificationDto();
        Page<Notification> page = new PageImpl<>(List.of(entity));
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(notificationMapper.fromEntityListToNotificationDtoList(List.of(entity))).thenReturn(List.of(dto));

        ApiMessageDto<ResponseListDto<List<NotificationDto>>> result =
                controller.autoComplete(new NotificationCriteria(), Pageable.unpaged());

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData().getContent()).containsExactly(dto);
    }

    @Test
    void shouldThrowNotFoundWhenDeleteIdDoesNotExist() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.delete(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_ERROR_NOT_FOUND);
    }

    @Test
    void shouldDeleteWhenIdExists() {
        Notification entity = new Notification();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(entity));

        ApiMessageDto<Void> result = controller.delete(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Delete notification success");
        verify(notificationRepository).delete(entity);
    }

    @Test
    void shouldThrowNotFoundWhenReadIdDoesNotExist() {
        ReadNotificationForm form = new ReadNotificationForm();
        form.setId(1L);
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.read(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_ERROR_NOT_FOUND);
    }

    @Test
    void shouldFlipStateToReadWhenCurrentStateIsSent() {
        ReadNotificationForm form = new ReadNotificationForm();
        form.setId(1L);
        Notification entity = new Notification();
        entity.setState(BaseConstant.NOTIFICATION_STATE_SENT);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(entity));

        ApiMessageDto<Void> result = controller.read(form, bindingResult);

        assertThat(entity.getState()).isEqualTo(BaseConstant.NOTIFICATION_STATE_READ);
        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Read notification success");
        verify(notificationRepository).save(entity);
    }

    @Test
    void shouldNotSaveWhenStateAlreadyRead() {
        ReadNotificationForm form = new ReadNotificationForm();
        form.setId(1L);
        Notification entity = new Notification();
        entity.setState(BaseConstant.NOTIFICATION_STATE_READ);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(entity));

        ApiMessageDto<Void> result = controller.read(form, bindingResult);

        assertThat(result.getResult()).isTrue();
        verify(notificationRepository, never()).save(any());
    }
}
