package logs.api.controller;

import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.ResponseListDto;
import logs.api.dto.notificationQuery.NotificationQueryDto;
import logs.api.exception.BadRequestException;
import logs.api.exception.NotFoundException;
import logs.api.form.notificationQuery.CreateNotificationQueryForm;
import logs.api.form.notificationQuery.NotificationQueryLinkForm;
import logs.api.mapper.NotificationQueryMapper;
import logs.api.model.Applications;
import logs.api.model.NotificationGroup;
import logs.api.model.NotificationQuery;
import logs.api.model.QueryTemplate;
import logs.api.model.criteria.NotificationQueryCriteria;
import logs.api.repository.ApplicationsRepository;
import logs.api.repository.NotificationGroupRepository;
import logs.api.repository.NotificationQueryRepository;
import logs.api.repository.QueryTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/notification-query")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class NotificationQueryController extends ABasicController {
    @Autowired
    private NotificationQueryRepository notificationQueryRepository;

    @Autowired
    private NotificationGroupRepository notificationGroupRepository;

    @Autowired
    private QueryTemplateRepository queryTemplateRepository;

    @Autowired
    private ApplicationsRepository applicationsRepository;

    @Autowired
    private NotificationQueryMapper notificationQueryMapper;

    @GetMapping(value = "/get/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOQ_V')")
    public ApiMessageDto<NotificationQueryDto> get(@PathVariable Long id) {
        NotificationQuery notificationQuery = notificationQueryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found notification query", ErrorCode.NOTIFICATION_QUERY_ERROR_NOT_FOUND));
        return makeSuccessResponse(notificationQueryMapper.fromEntityToNotificationQueryDto(notificationQuery), "Get notification query success");
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOQ_L')")
    public ApiMessageDto<ResponseListDto<List<NotificationQueryDto>>> list(NotificationQueryCriteria notificationQueryCriteria, @PageableDefault Pageable pageable) {
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<NotificationQuery> page = notificationQueryRepository.findAll(notificationQueryCriteria.getCriteria(), unsortedPageable);
        ResponseListDto<List<NotificationQueryDto>> responseListDto =
                makeResponseListDto(page, notificationQueryMapper::fromEntityListToNotificationQueryDtoList);
        return makeSuccessResponse(responseListDto, "Get list notification query success");
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOQ_C')")
    @Transactional
    public ApiMessageDto<Void> create(@Valid @RequestBody CreateNotificationQueryForm createNotificationQueryForm, BindingResult bindingResult) {
        NotificationGroup notificationGroup = notificationGroupRepository.findById(createNotificationQueryForm.getNotificationGroupId())
                .orElseThrow(() -> new NotFoundException("Not found notification group", ErrorCode.NOTIFICATION_GROUP_ERROR_NOT_FOUND));

        List<NotificationQueryLinkForm> links = createNotificationQueryForm.getLinks();

        Set<List<Long>> distinctPairs = links.stream()
                .map(link -> Arrays.asList(link.getQueryTemplateId(), link.getApplicationId()))
                .collect(Collectors.toSet());
        if (distinctPairs.size() != links.size()) {
            throw new BadRequestException("Duplicate query template and application in request");
        }

        if (links.isEmpty()) {
            notificationQueryRepository.deleteAllByNotificationGroupId(notificationGroup.getId());
        } else {
            List<Long> distinctQueryTemplateIds = links.stream()
                    .map(NotificationQueryLinkForm::getQueryTemplateId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, QueryTemplate> queryTemplateById = queryTemplateRepository.findAllById(distinctQueryTemplateIds).stream()
                    .collect(Collectors.toMap(QueryTemplate::getId, queryTemplate -> queryTemplate, (a, b) -> a));
            if (queryTemplateById.size() != distinctQueryTemplateIds.size()) {
                throw new NotFoundException("Not found query template", ErrorCode.QUERY_TEMPLATE_ERROR_NOT_FOUND);
            }

            List<Long> distinctApplicationIds = links.stream()
                    .map(NotificationQueryLinkForm::getApplicationId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, Applications> applicationsById = applicationsRepository.findAllById(distinctApplicationIds).stream()
                    .collect(Collectors.toMap(Applications::getId, applications -> applications, (a, b) -> a));
            if (applicationsById.size() != distinctApplicationIds.size()) {
                throw new NotFoundException("Not found application", ErrorCode.APPLICATIONS_ERROR_NOT_FOUND);
            }

            for (NotificationQueryLinkForm link : links) {
                QueryTemplate queryTemplate = queryTemplateById.get(link.getQueryTemplateId());
                if (queryTemplate.getApplication() != null
                        && !queryTemplate.getApplication().getId().equals(link.getApplicationId())) {
                    throw new BadRequestException("Query template's application does not match the requested application", ErrorCode.NOTIFICATION_QUERY_ERROR_APPLICATION_MISMATCH);
                }
            }

            Map<List<Long>, NotificationQuery> existingByPair = notificationQueryRepository.findAllByNotificationGroupId(notificationGroup.getId()).stream()
                    .collect(Collectors.toMap(
                            nq -> Arrays.asList(nq.getQueryTemplate().getId(), nq.getApplication().getId()),
                            nq -> nq,
                            (a, b) -> a));

            List<NotificationQuery> notificationQueriesToCreate = new ArrayList<>();
            for (NotificationQueryLinkForm link : links) {
                List<Long> pairKey = Arrays.asList(link.getQueryTemplateId(), link.getApplicationId());
                if (existingByPair.remove(pairKey) != null) {
                    continue;
                }
                NotificationQuery notificationQuery = new NotificationQuery();
                notificationQuery.setNotificationGroup(notificationGroup);
                notificationQuery.setQueryTemplate(queryTemplateById.get(link.getQueryTemplateId()));
                notificationQuery.setApplication(applicationsById.get(link.getApplicationId()));
                notificationQueriesToCreate.add(notificationQuery);
            }

            if (!existingByPair.isEmpty()) {
                notificationQueryRepository.deleteAll(existingByPair.values());
            }

            if (!notificationQueriesToCreate.isEmpty()) {
                notificationQueryRepository.saveAll(notificationQueriesToCreate);
            }
        }

        return makeSuccessResponse("Create notification query success");
    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('NOQ_D')")
    @Transactional
    public ApiMessageDto<Void> delete(@PathVariable Long id) {
        NotificationQuery notificationQuery = notificationQueryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Not found notification query", ErrorCode.NOTIFICATION_QUERY_ERROR_NOT_FOUND));
        notificationQueryRepository.delete(notificationQuery);
        return makeSuccessResponse("Delete notification query success");
    }
}
