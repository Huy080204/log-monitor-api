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
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/v1/application")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class ApplicationsController extends ABasicController {
    @Autowired
    private ApplicationsRepository applicationsRepository;

    @Autowired
    private ApplicationsMapper applicationsMapper;

    @Autowired
    private QueryTemplateRepository queryTemplateRepository;

    @Autowired
    private NotificationQueryRepository notificationQueryRepository;

    @GetMapping(value = "/get/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('APP_V')")
    public ApiMessageDto<ApplicationsDto> get(@PathVariable Long id) {
        Applications applications = applicationsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found applications!", ErrorCode.APPLICATIONS_ERROR_NOT_FOUND));
        return makeSuccessResponse(applicationsMapper.fromEntityToApplicationsDto(applications), "Get applications success");
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('APP_L')")
    public ApiMessageDto<ResponseListDto<List<ApplicationsDto>>> list(ApplicationsCriteria applicationsCriteria, @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        if (applicationsCriteria.getStatus() == null) {
            applicationsCriteria.setStatus(BaseConstant.STATUS_ACTIVE);
        }
        Page<Applications> page = applicationsRepository.findAll(applicationsCriteria.getCriteria(), pageable);
        ResponseListDto<List<ApplicationsDto>> responseListDto =
                makeResponseListDto(page, applicationsMapper::fromEntityToApplicationsDtoList);
        return makeSuccessResponse(responseListDto, "Get list success");
    }

    @GetMapping(value = "/auto-complete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<ResponseListDto<List<ApplicationsDto>>> autoComplete(ApplicationsCriteria applicationsCriteria, Pageable pageable) {
        applicationsCriteria.setStatus(BaseConstant.STATUS_ACTIVE);
        Page<Applications> page = applicationsRepository.findAll(applicationsCriteria.getCriteria(), pageable);
        ResponseListDto<List<ApplicationsDto>> responseListDto =
                makeResponseListDto(page, applicationsMapper::fromEntityToApplicationsAutoCompleteDtoList);
        return makeSuccessResponse(responseListDto, "Get auto complete applications success");
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('APP_C')")
    @Transactional
    public ApiMessageDto<Void> create(@Valid @RequestBody CreateApplicationsForm createApplicationsForm, BindingResult bindingResult) {
        if (applicationsRepository.existsByName(createApplicationsForm.getName())) {
            throw new BadRequestException("Applications name already exists", ErrorCode.APPLICATIONS_ERROR_NAME_EXISTED);
        }
        if (applicationsRepository.existsByVictoriaAppId(createApplicationsForm.getVictoriaAppId())) {
            throw new BadRequestException("Applications victoriaAppId already exists", ErrorCode.APPLICATIONS_ERROR_VICTORIA_APP_ID_EXISTED);
        }

        Applications applications = applicationsMapper.fromFormToEntity(createApplicationsForm);
        applicationsRepository.save(applications);
        return makeSuccessResponse("Create applications success");
    }

    @PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('APP_U')")
    @Transactional
    public ApiMessageDto<Void> update(@Valid @RequestBody UpdateApplicationsForm updateApplicationsForm, BindingResult bindingResult) {
        Applications applications = applicationsRepository.findById(updateApplicationsForm.getId())
                .orElseThrow(() -> new NotFoundException("Not found applications!", ErrorCode.APPLICATIONS_ERROR_NOT_FOUND));

        if (!Objects.equals(updateApplicationsForm.getName(), applications.getName())
                && applicationsRepository.existsByNameAndIdNot(updateApplicationsForm.getName(), applications.getId())) {
            throw new BadRequestException("Applications name already exists", ErrorCode.APPLICATIONS_ERROR_NAME_EXISTED);
        }
        if (!Objects.equals(updateApplicationsForm.getVictoriaAppId(), applications.getVictoriaAppId())
                && applicationsRepository.existsByVictoriaAppIdAndIdNot(updateApplicationsForm.getVictoriaAppId(), applications.getId())) {
            throw new BadRequestException("Applications victoriaAppId already exists", ErrorCode.APPLICATIONS_ERROR_VICTORIA_APP_ID_EXISTED);
        }

        applicationsMapper.updateEntityFromForm(updateApplicationsForm, applications);
        applicationsRepository.save(applications);
        return makeSuccessResponse("Update applications success");
    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('APP_D')")
    @Transactional
    public ApiMessageDto<Void> delete(@PathVariable Long id) {
        Applications applications = applicationsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found applications!", ErrorCode.APPLICATIONS_ERROR_NOT_FOUND));
        notificationQueryRepository.deleteAllByApplicationId(id);
        notificationQueryRepository.deleteAllByQueryTemplateApplicationId(id);
        queryTemplateRepository.deleteAllByApplicationId(id);
        applicationsRepository.delete(applications);
        return makeSuccessResponse("Delete applications success");
    }
}
