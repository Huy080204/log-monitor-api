package logs.api.controller;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.ResponseListDto;
import logs.api.dto.notificationRule.NotificationRuleDto;
import logs.api.exception.BadRequestException;
import logs.api.exception.NotFoundException;
import logs.api.form.notificationRule.CreateNotificationRuleForm;
import logs.api.form.notificationRule.UpdateNotificationRuleForm;
import logs.api.mapper.NotificationRuleMapper;
import logs.api.model.NotificationGroup;
import logs.api.model.NotificationRule;
import logs.api.model.criteria.NotificationRuleCriteria;
import logs.api.repository.NotificationGroupRepository;
import logs.api.repository.NotificationRuleRepository;
import logs.api.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRuleControllerTest {

    @Mock private NotificationRuleRepository notificationRuleRepository;
    @Mock private NotificationGroupRepository notificationGroupRepository;
    @Mock private NotificationRuleMapper notificationRuleMapper;
    @Mock private UserServiceImpl userService;
    @InjectMocks private NotificationRuleController controller;

    private final BindingResult bindingResult = mock(BindingResult.class);

    @Test
    void shouldCreateNotificationRuleWhenNotificationGroupCheckTypeIsComparison() {
        CreateNotificationRuleForm form = new CreateNotificationRuleForm();
        form.setName("rule-1");
        form.setDescription("desc");
        form.setNotificationGroupId(1L);

        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        group.setCheckType(BaseConstant.NOTIFICATION_GROUP_CHECK_TYPE_COMPARISON);

        NotificationRule entity = new NotificationRule();
        NotificationRuleDto dto = new NotificationRuleDto();

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(notificationRuleMapper.fromFormToEntity(form)).thenReturn(entity);
        when(notificationRuleMapper.fromEntityToNotificationRuleIdDto(entity)).thenReturn(dto);

        ApiMessageDto<NotificationRuleDto> result = controller.create(form, bindingResult);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(result.getMessage()).isEqualTo("Create notification rule success");
        assertThat(entity.getNotificationGroup()).isSameAs(group);
        verify(notificationRuleRepository).save(entity);
    }

    @Test
    void shouldThrowNotFoundWhenCreateNotificationGroupDoesNotExist() {
        CreateNotificationRuleForm form = new CreateNotificationRuleForm();
        form.setNotificationGroupId(1L);

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowBadRequestWhenCreateNotificationGroupCheckTypeIsThreshold() {
        CreateNotificationRuleForm form = new CreateNotificationRuleForm();
        form.setNotificationGroupId(1L);

        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        group.setCheckType(BaseConstant.NOTIFICATION_GROUP_CHECK_TYPE_THRESHOLD);

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_CHECK_TYPE_MISMATCH);
    }

    @Test
    void shouldThrowBadRequestWhenCreateNotificationGroupCheckTypeIsNull() {
        CreateNotificationRuleForm form = new CreateNotificationRuleForm();
        form.setNotificationGroupId(1L);

        NotificationGroup group = new NotificationGroup();
        group.setId(1L);

        when(notificationGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> controller.create(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_CHECK_TYPE_MISMATCH);
    }

    @Test
    void shouldUpdateNotificationRuleWhenIdExists() {
        UpdateNotificationRuleForm form = new UpdateNotificationRuleForm();
        form.setId(1L);
        form.setName("updated");
        form.setDescription("updated desc");

        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        group.setCheckType(BaseConstant.NOTIFICATION_GROUP_CHECK_TYPE_COMPARISON);

        NotificationRule entity = new NotificationRule();
        entity.setId(1L);
        entity.setNotificationGroup(group);

        when(notificationRuleRepository.findById(1L)).thenReturn(Optional.of(entity));

        ApiMessageDto<Void> result = controller.update(form, bindingResult);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Update notification rule success");
        verify(notificationRuleMapper).updateEntityFromForm(form, entity);
        verify(notificationRuleRepository).save(entity);
    }

    @Test
    void shouldThrowNotFoundWhenUpdateIdDoesNotExist() {
        UpdateNotificationRuleForm form = new UpdateNotificationRuleForm();
        form.setId(1L);

        when(notificationRuleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_RULE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldThrowBadRequestWhenUpdateNotificationGroupCheckTypeIsThreshold() {
        UpdateNotificationRuleForm form = new UpdateNotificationRuleForm();
        form.setId(1L);

        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        group.setCheckType(BaseConstant.NOTIFICATION_GROUP_CHECK_TYPE_THRESHOLD);

        NotificationRule entity = new NotificationRule();
        entity.setId(1L);
        entity.setNotificationGroup(group);

        when(notificationRuleRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_CHECK_TYPE_MISMATCH);
    }

    @Test
    void shouldThrowBadRequestWhenUpdateNotificationGroupCheckTypeIsNull() {
        UpdateNotificationRuleForm form = new UpdateNotificationRuleForm();
        form.setId(1L);

        NotificationGroup group = new NotificationGroup();
        group.setId(1L);

        NotificationRule entity = new NotificationRule();
        entity.setId(1L);
        entity.setNotificationGroup(group);

        when(notificationRuleRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> controller.update(form, bindingResult))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_GROUP_ERROR_CHECK_TYPE_MISMATCH);
    }

    @Test
    void shouldReturnPagedListWhenListCalled() {
        NotificationRuleCriteria criteria = mock(NotificationRuleCriteria.class);
        Specification<NotificationRule> specification = mock(Specification.class);
        when(criteria.getCriteria()).thenReturn(specification);
        Pageable pageable = PageRequest.of(0, 10);

        NotificationRule entity = new NotificationRule();
        Page<NotificationRule> page = new PageImpl<>(List.of(entity));
        when(notificationRuleRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(notificationRuleMapper.fromEntityToNotificationRuleDtoList(List.of(entity))).thenReturn(List.of(new NotificationRuleDto()));

        ApiMessageDto<ResponseListDto<List<NotificationRuleDto>>> result = controller.list(criteria, pageable);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Get list success");
        assertThat(result.getData().getContent()).hasSize(1);
        assertThat(result.getData().getTotalElements()).isEqualTo(1);
        assertThat(result.getData().getTotalPages()).isEqualTo(1);
        verify(notificationRuleRepository).findAll(specification, pageable);
    }

    @Test
    void shouldReturnNotificationRuleDtoWhenGetIdExists() {
        NotificationRule entity = new NotificationRule();
        entity.setId(1L);
        NotificationRuleDto dto = new NotificationRuleDto();

        when(notificationRuleRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(notificationRuleMapper.fromEntityToNotificationRuleDto(entity)).thenReturn(dto);

        ApiMessageDto<NotificationRuleDto> result = controller.get(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(result.getMessage()).isEqualTo("Get notification rule success");
    }

    @Test
    void shouldThrowNotFoundWhenGetIdDoesNotExist() {
        when(notificationRuleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.get(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.NOTIFICATION_RULE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldDeleteNotificationRuleWhenIdExists() {
        NotificationRule entity = new NotificationRule();
        entity.setId(1L);

        when(notificationRuleRepository.findById(1L)).thenReturn(Optional.of(entity));

        ApiMessageDto<Void> result = controller.delete(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Delete notification rule success");
        verify(notificationRuleRepository).delete(entity);
    }
}
