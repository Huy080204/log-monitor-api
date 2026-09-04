package logs.api.controller;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.ResponseListDto;
import logs.api.dto.queryTemplate.QueryTemplateDto;
import logs.api.exception.BadRequestException;
import logs.api.exception.NotFoundException;
import logs.api.form.queryTemplate.CreateQueryTemplateForm;
import logs.api.form.queryTemplate.UpdateQueryTemplateForm;
import logs.api.mapper.QueryTemplateMapper;
import logs.api.model.Applications;
import logs.api.model.QueryTemplate;
import logs.api.model.criteria.QueryTemplateCriteria;
import logs.api.repository.ApplicationsRepository;
import logs.api.repository.NotificationQueryRepository;
import logs.api.repository.QueryTemplateRepository;
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
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryTemplateControllerTest {

    @Mock
    private QueryTemplateRepository queryTemplateRepository;

    @Mock
    private ApplicationsRepository applicationsRepository;

    @Mock
    private QueryTemplateMapper queryTemplateMapper;

    @Mock
    private NotificationQueryRepository notificationQueryRepository;

    @InjectMocks
    private QueryTemplateController controller;

    @Test
    void shouldReturnQueryTemplateDtoWhenIdExists() {
        QueryTemplate entity = new QueryTemplate();
        QueryTemplateDto dto = new QueryTemplateDto();
        when(queryTemplateRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(queryTemplateMapper.fromEntityToQueryTemplateDto(entity)).thenReturn(dto);

        ApiMessageDto<QueryTemplateDto> result = controller.get(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(result.getMessage()).isEqualTo("Get query template success");
        verify(queryTemplateRepository).findById(1L);
    }

    @Test
    void shouldThrowNotFoundWhenQueryTemplateIdDoesNotExist() {
        when(queryTemplateRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.get(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldReturnListSuccess() {
        QueryTemplateCriteria criteria = new QueryTemplateCriteria();
        Pageable pageable = PageRequest.of(0, 10);
        QueryTemplate entity = new QueryTemplate();
        List<QueryTemplate> entityList = Collections.singletonList(entity);
        Page<QueryTemplate> page = new PageImpl<>(entityList, pageable, 1);
        QueryTemplateDto dto = new QueryTemplateDto();
        List<QueryTemplateDto> dtoList = Collections.singletonList(dto);
        when(queryTemplateRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(queryTemplateMapper.fromEntityToQueryTemplateDtoList(entityList)).thenReturn(dtoList);

        ApiMessageDto<ResponseListDto<List<QueryTemplateDto>>> result = controller.list(criteria, pageable);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData().getContent()).isSameAs(dtoList);
        assertThat(result.getData().getTotalElements()).isEqualTo(1);
        assertThat(result.getData().getTotalPages()).isEqualTo(1);
        assertThat(result.getMessage()).isEqualTo("Get list success");
    }

    @Test
    void shouldReturnAutoCompleteSuccess() {
        QueryTemplateCriteria criteria = new QueryTemplateCriteria();
        Pageable pageable = PageRequest.of(0, 10);
        QueryTemplate entity = new QueryTemplate();
        List<QueryTemplate> entityList = Collections.singletonList(entity);
        Page<QueryTemplate> page = new PageImpl<>(entityList, pageable, 1);
        QueryTemplateDto dto = new QueryTemplateDto();
        List<QueryTemplateDto> dtoList = Collections.singletonList(dto);
        when(queryTemplateRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(queryTemplateMapper.fromEntityListToQueryTemplateAutoCompleteDto(entityList)).thenReturn(dtoList);

        ApiMessageDto<ResponseListDto<List<QueryTemplateDto>>> result = controller.autoComplete(criteria, pageable);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData().getContent()).isSameAs(dtoList);
        assertThat(result.getMessage()).isEqualTo("Get auto complete query templates success");
    }

    @Test
    void shouldSetActiveStatusFilterWhenAutoComplete() {
        QueryTemplateCriteria criteria = new QueryTemplateCriteria();
        Pageable pageable = PageRequest.of(0, 10);
        QueryTemplate entity = new QueryTemplate();
        List<QueryTemplate> entityList = Collections.singletonList(entity);
        Page<QueryTemplate> page = new PageImpl<>(entityList, pageable, 1);
        QueryTemplateDto dto = new QueryTemplateDto();
        List<QueryTemplateDto> dtoList = Collections.singletonList(dto);
        when(queryTemplateRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(queryTemplateMapper.fromEntityListToQueryTemplateAutoCompleteDto(entityList)).thenReturn(dtoList);

        controller.autoComplete(criteria, pageable);

        assertThat(criteria.getStatus()).isEqualTo(BaseConstant.STATUS_ACTIVE);
    }

    @Test
    void shouldCreateQueryTemplateSuccessWhenApplicationIdIsNull() {
        CreateQueryTemplateForm form = new CreateQueryTemplateForm();
        form.setName("new-template");
        form.setApplicationId(null);
        QueryTemplate entity = new QueryTemplate();
        QueryTemplateDto idDto = new QueryTemplateDto();
        when(queryTemplateRepository.existsByNameAndApplicationIdIsNull("new-template")).thenReturn(false);
        when(queryTemplateMapper.fromFormToEntity(form)).thenReturn(entity);
        when(queryTemplateMapper.fromEntityToQueryTemplateIdDto(entity)).thenReturn(idDto);

        ApiMessageDto<QueryTemplateDto> result = controller.create(form, null);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(idDto);
        assertThat(result.getMessage()).isEqualTo("Create query template success");
        assertThat(entity.getApplication()).isNull();
        verify(queryTemplateRepository).save(entity);
    }

    @Test
    void shouldCreateQueryTemplateSuccessWhenApplicationIdProvided() {
        CreateQueryTemplateForm form = new CreateQueryTemplateForm();
        form.setName("new-template");
        form.setApplicationId(2L);
        QueryTemplate entity = new QueryTemplate();
        Applications application = new Applications();
        application.setId(2L);
        QueryTemplateDto idDto = new QueryTemplateDto();
        when(queryTemplateRepository.existsByNameAndApplicationId("new-template", 2L)).thenReturn(false);
        when(queryTemplateMapper.fromFormToEntity(form)).thenReturn(entity);
        when(applicationsRepository.findById(2L)).thenReturn(Optional.of(application));
        when(queryTemplateMapper.fromEntityToQueryTemplateIdDto(entity)).thenReturn(idDto);

        ApiMessageDto<QueryTemplateDto> result = controller.create(form, null);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(idDto);
        assertThat(entity.getApplication()).isSameAs(application);
        verify(queryTemplateRepository).save(entity);
    }

    @Test
    void shouldThrowBadRequestWhenNameExistedWithApplicationScope() {
        CreateQueryTemplateForm form = new CreateQueryTemplateForm();
        form.setName("dup-template");
        form.setApplicationId(2L);
        QueryTemplate entity = new QueryTemplate();
        Applications application = new Applications();
        application.setId(2L);
        when(queryTemplateMapper.fromFormToEntity(form)).thenReturn(entity);
        when(applicationsRepository.findById(2L)).thenReturn(Optional.of(application));
        when(queryTemplateRepository.existsByNameAndApplicationId("dup-template", 2L)).thenReturn(true);

        assertThatThrownBy(() -> controller.create(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.QUERY_TEMPLATE_ERROR_NAME_EXISTED);
    }

    @Test
    void shouldThrowBadRequestWhenNameExistedWithNullScope() {
        CreateQueryTemplateForm form = new CreateQueryTemplateForm();
        form.setName("dup-template");
        form.setApplicationId(null);
        when(queryTemplateRepository.existsByNameAndApplicationIdIsNull("dup-template")).thenReturn(true);

        assertThatThrownBy(() -> controller.create(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.QUERY_TEMPLATE_ERROR_NAME_EXISTED);
    }

    @Test
    void shouldThrowNotFoundWhenApplicationNotFoundOnCreate() {
        CreateQueryTemplateForm form = new CreateQueryTemplateForm();
        form.setName("new-template");
        form.setApplicationId(99L);
        QueryTemplate entity = new QueryTemplate();
        when(queryTemplateMapper.fromFormToEntity(form)).thenReturn(entity);
        when(applicationsRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.create(form, null))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.APPLICATIONS_ERROR_NOT_FOUND);
    }

    @Test
    void shouldUpdateQueryTemplateSuccessAndLeaveApplicationUnchanged() {
        UpdateQueryTemplateForm form = new UpdateQueryTemplateForm();
        form.setId(1L);
        form.setName("new-name");
        QueryTemplate entity = new QueryTemplate();
        entity.setId(1L);
        entity.setName("old-name");
        Applications currentApplication = new Applications();
        currentApplication.setId(2L);
        entity.setApplication(currentApplication);
        when(queryTemplateRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(queryTemplateRepository.existsByNameAndApplicationIdAndIdNot("new-name", 2L, 1L)).thenReturn(false);

        ApiMessageDto<Void> result = controller.update(form, null);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Update query template success");
        assertThat(entity.getApplication()).isSameAs(currentApplication);
        verify(applicationsRepository, never()).findById(any());
        verify(queryTemplateRepository).save(entity);
    }

    @Test
    void shouldThrowBadRequestWhenUpdateNameExistedWithApplicationScope() {
        UpdateQueryTemplateForm form = new UpdateQueryTemplateForm();
        form.setId(1L);
        form.setName("dup-template");
        QueryTemplate entity = new QueryTemplate();
        entity.setId(1L);
        entity.setName("old-name");
        Applications currentApplication = new Applications();
        currentApplication.setId(2L);
        entity.setApplication(currentApplication);
        when(queryTemplateRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(queryTemplateRepository.existsByNameAndApplicationIdAndIdNot("dup-template", 2L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> controller.update(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.QUERY_TEMPLATE_ERROR_NAME_EXISTED);
    }

    @Test
    void shouldThrowBadRequestWhenUpdateNameExistedWithNullScope() {
        UpdateQueryTemplateForm form = new UpdateQueryTemplateForm();
        form.setId(1L);
        form.setName("dup-template");
        QueryTemplate entity = new QueryTemplate();
        entity.setId(1L);
        entity.setName("old-name");
        when(queryTemplateRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(queryTemplateRepository.existsByNameAndApplicationIdIsNullAndIdNot("dup-template", 1L)).thenReturn(true);

        assertThatThrownBy(() -> controller.update(form, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.QUERY_TEMPLATE_ERROR_NAME_EXISTED);
    }

    @Test
    void shouldThrowNotFoundWhenQueryTemplateIdDoesNotExistOnUpdate() {
        UpdateQueryTemplateForm form = new UpdateQueryTemplateForm();
        form.setId(1L);
        when(queryTemplateRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.update(form, null))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND);
    }

    @Test
    void shouldSkipDuplicateNameCheckWhenUpdateNameUnchanged() {
        UpdateQueryTemplateForm form = new UpdateQueryTemplateForm();
        form.setId(1L);
        form.setName("same-name");
        QueryTemplate entity = new QueryTemplate();
        entity.setName("same-name");
        Applications currentApplication = new Applications();
        currentApplication.setId(2L);
        entity.setApplication(currentApplication);
        when(queryTemplateRepository.findById(1L)).thenReturn(Optional.of(entity));

        ApiMessageDto<Void> result = controller.update(form, null);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Update query template success");
        assertThat(entity.getApplication()).isSameAs(currentApplication);
        verify(applicationsRepository, never()).findById(any());
        verify(queryTemplateRepository, never()).existsByNameAndApplicationId(any(), any());
        verify(queryTemplateRepository, never()).existsByNameAndApplicationIdIsNull(any());
        verify(queryTemplateRepository, never()).existsByNameAndApplicationIdAndIdNot(any(), any(), any());
        verify(queryTemplateRepository, never()).existsByNameAndApplicationIdIsNullAndIdNot(any(), any());
        verify(queryTemplateRepository).save(entity);
    }

    @Test
    void shouldDeleteQueryTemplateSuccessAndCascadeDeleteNotificationQueriesInOrder() {
        QueryTemplate entity = new QueryTemplate();
        when(queryTemplateRepository.findById(1L)).thenReturn(Optional.of(entity));

        ApiMessageDto<Void> result = controller.delete(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Delete query template success");
        InOrder inOrder = inOrder(notificationQueryRepository, queryTemplateRepository);
        inOrder.verify(notificationQueryRepository).deleteAllByQueryTemplateId(1L);
        inOrder.verify(queryTemplateRepository).delete(entity);
    }

    @Test
    void shouldThrowNotFoundWhenQueryTemplateIdDoesNotExistOnDelete() {
        when(queryTemplateRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.delete(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND);
    }
}
