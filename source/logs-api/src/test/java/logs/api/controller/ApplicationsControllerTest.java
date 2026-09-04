package logs.api.controller;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.ResponseListDto;
import logs.api.dto.applications.ApplicationsDto;
import logs.api.exception.BadRequestException;
import logs.api.exception.NotFoundException;
import logs.api.form.applications.CreateApplicationsForm;
import logs.api.form.applications.UpdateApplicationsForm;
import logs.api.mapper.ApplicationsMapper;
import logs.api.model.Applications;
import logs.api.model.criteria.ApplicationsCriteria;
import logs.api.repository.ApplicationsRepository;
import logs.api.repository.NotificationQueryRepository;
import logs.api.repository.QueryTemplateRepository;
import logs.api.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationsControllerTest {

    @Mock
    private ApplicationsRepository applicationsRepository;

    @Mock
    private ApplicationsMapper applicationsMapper;

    @Mock
    private QueryTemplateRepository queryTemplateRepository;

    @Mock
    private NotificationQueryRepository notificationQueryRepository;

    @Mock
    private UserServiceImpl userService;

    @InjectMocks
    private ApplicationsController controller;

    @Test
    void shouldCreateApplicationsWhenNameNotExists() {
        CreateApplicationsForm form = new CreateApplicationsForm();
        form.setName("logs-api");
        form.setDescription("logs api service");
        Applications entity = new Applications();
        when(applicationsRepository.existsByName("logs-api")).thenReturn(false);
        when(applicationsMapper.fromFormToEntity(form)).thenReturn(entity);

        ApiMessageDto<Void> result = controller.create(form, null);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Create applications success");
        verify(applicationsRepository).save(entity);
    }

    @Test
    void shouldThrowDuplicateNameWhenCreateWithExistingName() {
        CreateApplicationsForm form = new CreateApplicationsForm();
        form.setName("logs-api");
        form.setDescription("logs api service");
        when(applicationsRepository.existsByName("logs-api")).thenReturn(true);

        assertThatThrownBy(() -> controller.create(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.APPLICATIONS_ERROR_NAME_EXISTED);
    }

    @Test
    void shouldThrowDuplicateNameWhenUpdateWithUnchangedName() {
        UpdateApplicationsForm form = new UpdateApplicationsForm();
        form.setId(1L);
        form.setName("logs-api");
        form.setDescription("logs api service updated");
        Applications entity = new Applications();
        entity.setName("logs-api");
        when(applicationsRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(applicationsRepository.existsByName("logs-api")).thenReturn(true);

        assertThatThrownBy(() -> controller.update(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.APPLICATIONS_ERROR_NAME_EXISTED);
    }

    @Test
    void shouldThrowDuplicateNameWhenUpdateWithCollidingName() {
        UpdateApplicationsForm form = new UpdateApplicationsForm();
        form.setId(1L);
        form.setName("other-app");
        form.setDescription("desc");
        Applications entity = new Applications();
        entity.setName("logs-api");
        when(applicationsRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(applicationsRepository.existsByName("other-app")).thenReturn(true);

        assertThatThrownBy(() -> controller.update(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.APPLICATIONS_ERROR_NAME_EXISTED);
    }

    @Test
    void shouldThrowDuplicateVictoriaAppIdWhenCreateWithExisting() {
        CreateApplicationsForm form = new CreateApplicationsForm();
        form.setName("logs-api");
        form.setVictoriaAppId("app-1");
        form.setDescription("logs api service");
        when(applicationsRepository.existsByName("logs-api")).thenReturn(false);
        when(applicationsRepository.existsByVictoriaAppId("app-1")).thenReturn(true);

        assertThatThrownBy(() -> controller.create(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.APPLICATIONS_ERROR_VICTORIA_APP_ID_EXISTED);
    }

    @Test
    void shouldThrowDuplicateVictoriaAppIdWhenUpdateWithCollidingValue() {
        UpdateApplicationsForm form = new UpdateApplicationsForm();
        form.setId(1L);
        form.setName("logs-api");
        form.setVictoriaAppId("app-2");
        form.setDescription("desc");
        Applications entity = new Applications();
        entity.setName("logs-api");
        entity.setVictoriaAppId("app-1");
        when(applicationsRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(applicationsRepository.existsByVictoriaAppId("app-2")).thenReturn(true);

        assertThatThrownBy(() -> controller.update(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.APPLICATIONS_ERROR_VICTORIA_APP_ID_EXISTED);
    }

    @Test
    void shouldAllowUpdateWhenVictoriaAppIdUnchanged() {
        UpdateApplicationsForm form = new UpdateApplicationsForm();
        form.setId(1L);
        form.setName("logs-api");
        form.setVictoriaAppId("app-1");
        form.setDescription("desc");
        Applications entity = new Applications();
        entity.setName("logs-api");
        entity.setVictoriaAppId("app-1");
        when(applicationsRepository.findById(1L)).thenReturn(Optional.of(entity));

        ApiMessageDto<Void> result = controller.update(form, null);

        assertThat(result.getResult()).isTrue();
        verify(applicationsRepository, never()).existsByVictoriaAppId(anyString());
        verify(applicationsRepository).save(entity);
    }

    @Test
    void shouldReturnApplicationsDtoWhenIdExists() {
        Applications entity = new Applications();
        ApplicationsDto dto = new ApplicationsDto();
        when(applicationsRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(applicationsMapper.fromEntityToApplicationsDto(entity)).thenReturn(dto);

        ApiMessageDto<ApplicationsDto> result = controller.get(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(result.getMessage()).isEqualTo("Get applications success");
    }

    @Test
    void shouldThrowNotFoundWhenApplicationsIdDoesNotExistOnGet() {
        when(applicationsRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.get(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.APPLICATIONS_ERROR_NOT_FOUND);
    }

    @Test
    void shouldDeleteApplicationsWhenIdExists() {
        Applications entity = new Applications();
        when(applicationsRepository.findById(1L)).thenReturn(Optional.of(entity));

        ApiMessageDto<Void> result = controller.delete(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Delete applications success");
        verify(applicationsRepository).delete(entity);
    }

    @Test
    void shouldCascadeDeleteChildrenInOrderWhenDeletingApplications() {
        Applications entity = new Applications();
        when(applicationsRepository.findById(1L)).thenReturn(Optional.of(entity));

        controller.delete(1L);

        InOrder inOrder = inOrder(notificationQueryRepository, queryTemplateRepository, applicationsRepository);
        inOrder.verify(notificationQueryRepository).deleteAllByApplicationId(1L);
        inOrder.verify(notificationQueryRepository).deleteAllByQueryTemplateApplicationId(1L);
        inOrder.verify(queryTemplateRepository).deleteAllByApplicationId(1L);
        inOrder.verify(applicationsRepository).delete(entity);
    }

    @Test
    void shouldThrowNotFoundWhenApplicationsIdDoesNotExistOnDelete() {
        when(applicationsRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.delete(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.APPLICATIONS_ERROR_NOT_FOUND);
    }

    @Test
    void shouldReturnFilteredListWhenCriteriaProvided() {
        ApplicationsCriteria criteria = new ApplicationsCriteria();
        criteria.setId(1L);
        criteria.setStatus(1);
        criteria.setName("logs-api");
        Pageable pageable = PageRequest.of(0, 10);
        Applications entity = new Applications();
        List<Applications> entityList = Collections.singletonList(entity);
        Page<Applications> page = new PageImpl<>(entityList, pageable, 1);
        List<ApplicationsDto> dtoList = Collections.singletonList(new ApplicationsDto());
        when(applicationsRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(applicationsMapper.fromEntityToApplicationsDtoList(entityList)).thenReturn(dtoList);

        ApiMessageDto<ResponseListDto<List<ApplicationsDto>>> result = controller.list(criteria, pageable);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData().getContent()).isSameAs(dtoList);
        assertThat(result.getData().getTotalElements()).isEqualTo(1);
        assertThat(result.getData().getTotalPages()).isEqualTo(1);
    }

    @Test
    void shouldDefaultStatusActiveWhenListCriteriaStatusOmitted() {
        ApplicationsCriteria criteria = new ApplicationsCriteria();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Applications> page = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(applicationsRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(applicationsMapper.fromEntityToApplicationsDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        controller.list(criteria, pageable);

        assertThat(criteria.getStatus()).isEqualTo(BaseConstant.STATUS_ACTIVE);
    }

    @Test
    void shouldReturnOnlyIdAndNameWhenAutoCompleteActiveRecords() {
        ApplicationsCriteria criteria = new ApplicationsCriteria();
        Pageable pageable = PageRequest.of(0, 10);
        Applications entity = new Applications();
        List<Applications> entityList = Collections.singletonList(entity);
        Page<Applications> page = new PageImpl<>(entityList, pageable, 1);
        ApplicationsDto dto = new ApplicationsDto();
        dto.setId(1L);
        dto.setName("logs-api");
        List<ApplicationsDto> dtoList = Collections.singletonList(dto);
        when(applicationsRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(applicationsMapper.fromEntityToApplicationsAutoCompleteDtoList(entityList)).thenReturn(dtoList);

        ApiMessageDto<ResponseListDto<List<ApplicationsDto>>> result = controller.autoComplete(criteria, pageable);

        assertThat(criteria.getStatus()).isEqualTo(BaseConstant.STATUS_ACTIVE);
        assertThat(result.getData().getContent()).isSameAs(dtoList);
        assertThat(result.getData().getContent()).hasSize(1);
        ApplicationsDto returned = result.getData().getContent().get(0);
        assertThat(returned.getId()).isEqualTo(1L);
        assertThat(returned.getName()).isEqualTo("logs-api");
    }
}
