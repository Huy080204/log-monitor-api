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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/v1/query-template")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class QueryTemplateController extends ABasicController {
    @Autowired
    private QueryTemplateRepository queryTemplateRepository;

    @Autowired
    private ApplicationsRepository applicationsRepository;

    @Autowired
    private QueryTemplateMapper queryTemplateMapper;

    @Autowired
    private NotificationQueryRepository notificationQueryRepository;

    @GetMapping(value = "/get/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('QTP_V')")
    public ApiMessageDto<QueryTemplateDto> get(@PathVariable Long id) {
        QueryTemplate queryTemplate = queryTemplateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Query template not found", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND));
        return makeSuccessResponse(queryTemplateMapper.fromEntityToQueryTemplateDto(queryTemplate), "Get query template success");
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('QTP_L')")
    public ApiMessageDto<ResponseListDto<List<QueryTemplateDto>>> list(QueryTemplateCriteria queryTemplateCriteria, @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<QueryTemplate> page = queryTemplateRepository.findAll(queryTemplateCriteria.getCriteria(), pageable);
        ResponseListDto<List<QueryTemplateDto>> responseListDto =
                makeResponseListDto(page, queryTemplateMapper::fromEntityToQueryTemplateDtoList);
        return makeSuccessResponse(responseListDto, "Get list success");
    }

    @GetMapping(value = "/auto-complete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<ResponseListDto<List<QueryTemplateDto>>> autoComplete(QueryTemplateCriteria queryTemplateCriteria, Pageable pageable) {
        queryTemplateCriteria.setStatus(BaseConstant.STATUS_ACTIVE);
        Page<QueryTemplate> page = queryTemplateRepository.findAll(queryTemplateCriteria.getCriteria(), pageable);
        ResponseListDto<List<QueryTemplateDto>> responseListDto =
                makeResponseListDto(page, queryTemplateMapper::fromEntityListToQueryTemplateAutoCompleteDto);
        return makeSuccessResponse(responseListDto, "Get auto complete query templates success");
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('QTP_C')")
    @Transactional
    public ApiMessageDto<QueryTemplateDto> create(@Valid @RequestBody CreateQueryTemplateForm createQueryTemplateForm, BindingResult bindingResult) {
        QueryTemplate queryTemplate = queryTemplateMapper.fromFormToEntity(createQueryTemplateForm);
        if (createQueryTemplateForm.getApplicationId() != null) {
            Applications applications = applicationsRepository.findById(createQueryTemplateForm.getApplicationId())
                    .orElseThrow(() -> new NotFoundException("Application not found", ErrorCode.APPLICATIONS_ERROR_NOT_FOUND));
            queryTemplate.setApplication(applications);
        }
        checkDuplicateName(createQueryTemplateForm.getName(), createQueryTemplateForm.getApplicationId());

        queryTemplateRepository.save(queryTemplate);
        return makeSuccessResponse(queryTemplateMapper.fromEntityToQueryTemplateIdDto(queryTemplate), "Create query template success");
    }

    @PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('QTP_U')")
    @Transactional
    public ApiMessageDto<Void> update(@Valid @RequestBody UpdateQueryTemplateForm updateQueryTemplateForm, BindingResult bindingResult) {
        QueryTemplate queryTemplate = queryTemplateRepository.findById(updateQueryTemplateForm.getId())
                .orElseThrow(() -> new NotFoundException("Query template not found", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND));

        boolean nameChanged = !Objects.equals(queryTemplate.getName(), updateQueryTemplateForm.getName());
        Long currentApplicationId = queryTemplate.getApplication() != null ? queryTemplate.getApplication().getId() : null;
        boolean applicationChanged = !Objects.equals(currentApplicationId, updateQueryTemplateForm.getApplicationId());

        queryTemplateMapper.updateEntityFromForm(updateQueryTemplateForm, queryTemplate);
        if (updateQueryTemplateForm.getApplicationId() != null) {
            Applications applications = applicationsRepository.findById(updateQueryTemplateForm.getApplicationId())
                    .orElseThrow(() -> new NotFoundException("Application not found", ErrorCode.APPLICATIONS_ERROR_NOT_FOUND));
            queryTemplate.setApplication(applications);
        } else {
            queryTemplate.setApplication(null);
        }
        if (nameChanged || applicationChanged) {
            checkDuplicateName(updateQueryTemplateForm.getName(), updateQueryTemplateForm.getApplicationId());
        }

        queryTemplateRepository.save(queryTemplate);
        return makeSuccessResponse("Update query template success");
    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('QTP_D')")
    @Transactional
    public ApiMessageDto<Void> delete(@PathVariable Long id) {
        QueryTemplate queryTemplate = queryTemplateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Query template not found", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND));
        notificationQueryRepository.deleteAllByQueryTemplateId(id);
        queryTemplateRepository.delete(queryTemplate);
        return makeSuccessResponse("Delete query template success");
    }

    private void checkDuplicateName(String name, Long applicationId) {
        boolean existed = applicationId != null
                ? queryTemplateRepository.existsByNameAndApplicationId(name, applicationId)
                : queryTemplateRepository.existsByNameAndApplicationIdIsNull(name);
        if (existed) {
            throw new BadRequestException("Query template name already exist", ErrorCode.QUERY_TEMPLATE_ERROR_NAME_EXISTED);
        }
    }
}
