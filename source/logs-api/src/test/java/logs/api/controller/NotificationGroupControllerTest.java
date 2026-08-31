package logs.api.controller;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.ResponseListDto;
import logs.api.dto.notificationGroup.NotificationGroupDto;
import logs.api.dto.setting.SettingNotificationChannelDto;
import logs.api.exception.BadRequestException;
import logs.api.exception.NotFoundException;
import logs.api.form.notificationGroup.CreateNotificationGroupForm;
import logs.api.form.notificationGroup.UpdateNotificationGroupForm;
import logs.api.mapper.NotificationGroupMapper;
import logs.api.model.NotificationGroup;
import logs.api.model.criteria.NotificationGroupCriteria;
import logs.api.repository.NotificationGroupRepository;
import logs.api.repository.NotificationQueryRepository;
import logs.api.repository.NotificationRepository;
import logs.api.service.NotificationService;
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
import static org.mockito.Mockito.times;
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
    private NotificationService notificationService;
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
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationGroupMapper.fromEntityToNotificationGroupDto(entity)).thenReturn(dto);

        ApiMessageDto<NotificationGroupDto> result = controller.get(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(result.getMessage()).isEqualTo("Get notification group success");
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
    void shouldDeriveTypeFromChannelSettingWhenCreateFormTypeIsNull() {
        CreateNotificationGroupForm form = new CreateNotificationGroupForm();
        form.setName("Team Alerts");
        form.setChannelSetting("{\"type\":1,\"channel\":\"#alerts\"}");
        form.setType(null);
        NotificationGroup entity = new NotificationGroup();
        entity.setChannelSetting(form.getChannelSetting());
        SettingNotificationChannelDto parsed = new SettingNotificationChannelDto();
        parsed.setType(BaseConstant.NOTIFICATION_CHANNEL_TYPE_SLACK);
        NotificationGroupDto idDto = new NotificationGroupDto();
        when(notificationGroupRepository.existsByName("Team Alerts")).thenReturn(false);
        when(notificationGroupMapper.fromFormToEntity(form)).thenReturn(entity);
        when(notificationService.parseChannelSetting(entity.getChannelSetting())).thenReturn(parsed);
        when(notificationGroupMapper.fromEntityToNotificationGroupIdDto(entity)).thenReturn(idDto);

        ApiMessageDto<NotificationGroupDto> result = controller.create(form, bindingResult);

        assertThat(entity.getType()).isEqualTo(BaseConstant.NOTIFICATION_CHANNEL_TYPE_SLACK);
        assertThat(entity.getStatus()).isEqualTo(BaseConstant.STATUS_PENDING);
        assertThat(result.getData()).isSameAs(idDto);
        assertThat(result.getMessage()).isEqualTo("Create notification group success");
        verify(notificationGroupRepository).save(entity);
    }

    @Test
    void shouldKeepFormTypeWhenCreateFormTypeIsProvided() {
        CreateNotificationGroupForm form = new CreateNotificationGroupForm();
        form.setName("Team Alerts");
        form.setType(BaseConstant.NOTIFICATION_CHANNEL_TYPE_TELEGRAM);
        NotificationGroup entity = new NotificationGroup();
        entity.setType(BaseConstant.NOTIFICATION_CHANNEL_TYPE_TELEGRAM);
        when(notificationGroupRepository.existsByName("Team Alerts")).thenReturn(false);
        when(notificationGroupMapper.fromFormToEntity(form)).thenReturn(entity);
        when(notificationGroupMapper.fromEntityToNotificationGroupIdDto(entity)).thenReturn(new NotificationGroupDto());

        controller.create(form, bindingResult);

        assertThat(entity.getType()).isEqualTo(BaseConstant.NOTIFICATION_CHANNEL_TYPE_TELEGRAM);
        verify(notificationService, times(0)).parseChannelSetting(any());
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
    void shouldDeriveTypeFromChannelSettingWhenUpdateFormTypeIsNull() {
        UpdateNotificationGroupForm form = new UpdateNotificationGroupForm();
        form.setId(1L);
        form.setName("Old Name");
        form.setType(null);
        NotificationGroup entity = new NotificationGroup();
        entity.setId(1L);
        entity.setName("Old Name");
        entity.setChannelSetting("{\"type\":0}");
        SettingNotificationChannelDto parsed = new SettingNotificationChannelDto();
        parsed.setType(BaseConstant.NOTIFICATION_CHANNEL_TYPE_TELEGRAM);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationService.parseChannelSetting(entity.getChannelSetting())).thenReturn(parsed);

        ApiMessageDto<Void> result = controller.update(form, bindingResult);

        assertThat(entity.getType()).isEqualTo(BaseConstant.NOTIFICATION_CHANNEL_TYPE_TELEGRAM);
        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Update notification group success");
        verify(notificationGroupRepository).save(entity);
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
        InOrder inOrder = inOrder(notificationRepository, notificationQueryRepository, notificationGroupRepository);
        inOrder.verify(notificationRepository).deleteAllByNotificationGroupId(1L);
        inOrder.verify(notificationQueryRepository).deleteAllByNotificationGroupId(1L);
        inOrder.verify(notificationGroupRepository).delete(entity);
    }

    @Test
    void shouldThrowNotFoundWhenActivateIdDoesNotExist() {
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.activate(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND);
    }

    @Test
    void shouldActivateWithoutFlippingWhenNoGroupIsCurrentlyActive() {
        NotificationGroup target = new NotificationGroup();
        target.setId(1L);
        target.setStatus(BaseConstant.STATUS_PENDING);
        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(target));
        when(notificationGroupRepository.findFirstByStatus(BaseConstant.STATUS_ACTIVE)).thenReturn(Optional.empty());

        controller.activate(1L);

        assertThat(target.getStatus()).isEqualTo(BaseConstant.STATUS_ACTIVE);
        verify(notificationGroupRepository, times(1)).save(target);
    }

    @Test
    void shouldFlipPreviousActiveGroupToPendingWhenActivatingAnotherGroup() {
        NotificationGroup target = new NotificationGroup();
        target.setId(2L);
        target.setStatus(BaseConstant.STATUS_PENDING);
        NotificationGroup currentActive = new NotificationGroup();
        currentActive.setId(1L);
        currentActive.setStatus(BaseConstant.STATUS_ACTIVE);
        when(notificationGroupRepository.findById(2L)).thenReturn(Optional.of(target));
        when(notificationGroupRepository.findFirstByStatus(BaseConstant.STATUS_ACTIVE)).thenReturn(Optional.of(currentActive));

        controller.activate(2L);

        assertThat(currentActive.getStatus()).isEqualTo(BaseConstant.STATUS_PENDING);
        assertThat(target.getStatus()).isEqualTo(BaseConstant.STATUS_ACTIVE);
        // NotificationGroup inherits ReuseId's Lombok @Data equals(), which compares only
        // `reusedId` (unset here) — currentActive.equals(target) is true, so a bare-argument
        // verify() would ambiguously match either save() call. same() pins each to its own call.
        verify(notificationGroupRepository).save(same(currentActive));
        verify(notificationGroupRepository).save(same(target));
    }
}
