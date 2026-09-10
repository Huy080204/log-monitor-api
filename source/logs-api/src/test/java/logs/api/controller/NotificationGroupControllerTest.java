package logs.api.controller;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.ResponseListDto;
import logs.api.dto.notificationChannel.NotificationChannelDto;
import logs.api.dto.notificationGroup.NotificationGroupDto;
import logs.api.exception.BadRequestException;
import logs.api.exception.NotFoundException;
import logs.api.form.notificationGroup.CreateNotificationGroupForm;
import logs.api.form.notificationGroup.UpdateNotificationGroupForm;
import logs.api.mapper.NotificationGroupMapper;
import logs.api.model.NotificationChannel;
import logs.api.model.NotificationGroup;
import logs.api.model.criteria.NotificationGroupCriteria;
import logs.api.repository.NotificationChannelRepository;
import logs.api.repository.NotificationGroupRepository;
import logs.api.repository.NotificationQueryRepository;
import logs.api.repository.NotificationRepository;
import logs.api.repository.NotificationRuleRepository;
import logs.api.service.QuartzSchedulerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationGroupControllerTest {

    @Mock
    private NotificationGroupRepository notificationGroupRepository;
    @Mock
    private NotificationGroupMapper notificationGroupMapper;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationQueryRepository notificationQueryRepository;
    @Mock
    private NotificationRuleRepository notificationRuleRepository;
    @Mock
    private NotificationChannelRepository notificationChannelRepository;
    @Mock
    private QuartzSchedulerService quartzSchedulerService;
    @InjectMocks
    private NotificationGroupController controller;

    private final BindingResult bindingResult = mock(BindingResult.class);

    @Test
    void shouldThrowNotFoundWhenGetIdDoesNotExist() {
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.get(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND);
    }

    @Test
    void shouldReturnNotificationGroupDtoWhenGetIdExists() {
        NotificationGroup entity = new NotificationGroup();
        NotificationGroupDto dto = new NotificationGroupDto();
        NotificationChannelDto channelDto = new NotificationChannelDto();
        channelDto.setId(5L);
        channelDto.setName("Slack Alerts");
        dto.setNotificationChannel(channelDto);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationGroupMapper.fromEntityToNotificationGroupDto(entity)).thenReturn(dto);

        ApiMessageDto<NotificationGroupDto> result = controller.get(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(result.getMessage()).isEqualTo("Get notification group success");
        assertThat(result.getData().getNotificationChannel().getId()).isEqualTo(5L);
        assertThat(result.getData().getNotificationChannel().getName()).isEqualTo("Slack Alerts");
    }

    @Test
    void shouldReturnPagedListWhenListCalled() {
        NotificationGroup entity = new NotificationGroup();
        NotificationGroupDto dto = new NotificationGroupDto();
        Page<NotificationGroup> page = new PageImpl<>(List.of(entity));
        when(notificationGroupRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(notificationGroupMapper.fromEntityListToNotificationGroupDtoList(List.of(entity))).thenReturn(List.of(dto));

        ApiMessageDto<ResponseListDto<List<NotificationGroupDto>>> result =
                controller.list(new NotificationGroupCriteria(), Pageable.unpaged());

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData().getContent()).containsExactly(dto);
        assertThat(result.getData().getTotalElements()).isEqualTo(1);
        assertThat(result.getData().getTotalPages()).isEqualTo(page.getTotalPages());
    }

    @Test
    void shouldFilterByActiveStatusWhenAutoCompleteCalled() {
        NotificationGroup entity = new NotificationGroup();
        NotificationGroupDto dto = new NotificationGroupDto();
        NotificationGroupCriteria criteria = new NotificationGroupCriteria();
        Page<NotificationGroup> page = new PageImpl<>(List.of(entity));
        when(notificationGroupRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(notificationGroupMapper.fromEntityListToNotificationGroupDtoAutoCompleteList(List.of(entity))).thenReturn(List.of(dto));

        controller.autoComplete(criteria, Pageable.unpaged());

        assertThat(criteria.getStatus()).isEqualTo(BaseConstant.STATUS_ACTIVE);
    }

    @Test
    void shouldThrowBadRequestWhenCreateNameExisted() {
        CreateNotificationGroupForm form = new CreateNotificationGroupForm();
        form.setName("Team Alerts");
        when(notificationGroupRepository.existsByName("Team Alerts")).thenReturn(true);

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_NAME_EXISTED);
    }

    @Test
    void shouldThrowNotFoundWhenUpdateIdDoesNotExist() {
        UpdateNotificationGroupForm form = new UpdateNotificationGroupForm();
        form.setId(1L);
        form.setName("Team Alerts");
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowBadRequestWhenUpdateNameExisted() {
        UpdateNotificationGroupForm form = new UpdateNotificationGroupForm();
        form.setId(1L);
        form.setName("New Name");
        NotificationGroup entity = new NotificationGroup();
        entity.setId(1L);
        entity.setName("Old Name");
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationGroupRepository.existsByName("New Name")).thenReturn(true);

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_NAME_EXISTED);
    }

    @Test
    void shouldThrowNotFoundWhenDeleteIdDoesNotExist() {
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.delete(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowBadRequestWhenDeleteActiveGroup() {
        NotificationGroup entity = new NotificationGroup();
        entity.setId(1L);
        entity.setStatus(BaseConstant.STATUS_ACTIVE);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> controller.delete(1L))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_DELETE_ACTIVE);
    }

    @Test
    void shouldCascadeDeleteChildrenInOrderWhenDeletingNonActiveGroup() {
        NotificationGroup entity = new NotificationGroup();
        entity.setId(1L);
        entity.setStatus(BaseConstant.STATUS_PENDING);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(entity));

        ApiMessageDto<Void> result = controller.delete(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Delete notification group success");
        InOrder inOrder = inOrder(notificationRepository, notificationQueryRepository, notificationRuleRepository, notificationGroupRepository);
        inOrder.verify(notificationRepository).deleteAllByNotificationGroupId(1L);
        inOrder.verify(notificationQueryRepository).deleteAllByNotificationGroupId(1L);
        inOrder.verify(notificationRuleRepository).deleteAllByNotificationGroupId(1L);
        inOrder.verify(notificationGroupRepository).delete(entity);
    }

    @Test
    void shouldThrowNotFoundWhenCreateNotificationChannelIdDoesNotExist() {
        CreateNotificationGroupForm form = new CreateNotificationGroupForm();
        form.setName("Team Alerts");
        form.setNotificationChannelId(99L);
        when(notificationChannelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_CHANNEL_ERROR_NOT_FOUND);
    }

    @Test
    void shouldSetNotificationChannelWhenCreateNotificationChannelIdExists() {
        CreateNotificationGroupForm form = new CreateNotificationGroupForm();
        form.setName("Team Alerts");
        form.setNotificationChannelId(5L);
        NotificationChannel channel = new NotificationChannel();
        channel.setId(5L);
        NotificationGroup entity = new NotificationGroup();
        when(notificationGroupRepository.existsByName("Team Alerts")).thenReturn(false);
        when(notificationChannelRepository.findById(5L)).thenReturn(Optional.of(channel));
        when(notificationGroupMapper.fromFormToEntity(form)).thenReturn(entity);
        when(notificationGroupMapper.fromEntityToNotificationGroupIdDto(entity)).thenReturn(new NotificationGroupDto());

        controller.create(form, bindingResult);

        assertThat(entity.getNotificationChannel()).isSameAs(channel);
        verify(notificationGroupRepository).save(entity);
    }

    @Test
    void shouldThrowNotFoundWhenUpdateNotificationChannelIdDoesNotExist() {
        UpdateNotificationGroupForm form = new UpdateNotificationGroupForm();
        form.setId(1L);
        form.setName("Team Alerts");
        form.setNotificationChannelId(99L);
        NotificationGroup entity = new NotificationGroup();
        entity.setId(1L);
        entity.setName("Team Alerts");
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationChannelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_CHANNEL_ERROR_NOT_FOUND);
    }

    @Test
    void shouldSetNotificationChannelWhenUpdateNotificationChannelIdExists() {
        UpdateNotificationGroupForm form = new UpdateNotificationGroupForm();
        form.setId(1L);
        form.setName("Old Name");
        form.setNotificationChannelId(5L);
        NotificationGroup entity = new NotificationGroup();
        entity.setId(1L);
        entity.setName("Old Name");
        NotificationChannel channel = new NotificationChannel();
        channel.setId(5L);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationChannelRepository.findById(5L)).thenReturn(Optional.of(channel));

        controller.update(form, bindingResult);

        assertThat(entity.getNotificationChannel()).isSameAs(channel);
        verify(notificationGroupRepository).save(entity);
    }

}
