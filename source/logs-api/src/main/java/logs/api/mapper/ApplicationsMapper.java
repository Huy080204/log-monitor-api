package logs.api.mapper;

import logs.api.dto.applications.ApplicationsDto;
import logs.api.form.applications.CreateApplicationsForm;
import logs.api.form.applications.UpdateApplicationsForm;
import logs.api.model.Applications;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ApplicationsMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "modifiedDate", target = "modifiedDate")
    @Mapping(source = "createdDate", target = "createdDate")
    @Mapping(source = "status", target = "status")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToApplicationsDto")
    ApplicationsDto fromEntityToApplicationsDto(Applications applications);

    @IterableMapping(elementTargetType = ApplicationsDto.class, qualifiedByName = "fromEntityToApplicationsDto")
    List<ApplicationsDto> fromEntityToApplicationsDtoList(List<Applications> applicationsList);

    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @BeanMapping(ignoreByDefault = true)
    @Named("createMapping")
    Applications fromFormToEntity(CreateApplicationsForm createApplicationsForm);

    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @BeanMapping(ignoreByDefault = true)
    @Named("updateMapping")
    void updateEntityFromForm(UpdateApplicationsForm updateApplicationsForm, @MappingTarget Applications applications);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToApplicationsAutoCompleteDto")
    ApplicationsDto fromEntityToApplicationsAutoCompleteDto(Applications applications);

    @IterableMapping(elementTargetType = ApplicationsDto.class, qualifiedByName = "fromEntityToApplicationsAutoCompleteDto")
    List<ApplicationsDto> fromEntityToApplicationsAutoCompleteDtoList(List<Applications> applicationsList);
}
