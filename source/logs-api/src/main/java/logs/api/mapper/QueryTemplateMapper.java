package logs.api.mapper;

import logs.api.dto.queryTemplate.QueryTemplateDto;
import logs.api.form.queryTemplate.CreateQueryTemplateForm;
import logs.api.form.queryTemplate.UpdateQueryTemplateForm;
import logs.api.model.QueryTemplate;
import org.mapstruct.BeanMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {ApplicationsMapper.class})
public interface QueryTemplateMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "query", target = "query")
    @Mapping(source = "count", target = "count")
    @Mapping(source = "application", target = "application", qualifiedByName = "fromEntityToApplicationsDto")
    @Mapping(source = "modifiedDate", target = "modifiedDate")
    @Mapping(source = "createdDate", target = "createdDate")
    @Mapping(source = "status", target = "status")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToQueryTemplateDto")
    QueryTemplateDto fromEntityToQueryTemplateDto(QueryTemplate queryTemplate);

    @IterableMapping(elementTargetType = QueryTemplateDto.class, qualifiedByName = "fromEntityToQueryTemplateDto")
    List<QueryTemplateDto> fromEntityToQueryTemplateDtoList(List<QueryTemplate> queryTemplates);

    @Mapping(source = "name", target = "name")
    @Mapping(source = "query", target = "query")
    @Mapping(source = "count", target = "count")
    @BeanMapping(ignoreByDefault = true)
    @Named("adminCreateMapping")
    QueryTemplate fromFormToEntity(CreateQueryTemplateForm createQueryTemplateForm);

    @Mapping(source = "name", target = "name")
    @Mapping(source = "query", target = "query")
    @Mapping(source = "count", target = "count")
    @Mapping(source = "status", target = "status")
    @BeanMapping(ignoreByDefault = true)
    @Named("adminUpdateMapping")
    void updateEntityFromForm(UpdateQueryTemplateForm updateQueryTemplateForm, @MappingTarget QueryTemplate queryTemplate);

    @Mapping(source = "id", target = "id")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToQueryTemplateIdDto")
    QueryTemplateDto fromEntityToQueryTemplateIdDto(QueryTemplate queryTemplate);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @BeanMapping(ignoreByDefault = true)
    @Named("autoCompleteMapping")
    QueryTemplateDto fromEntityToQueryTemplateAutoCompleteDto(QueryTemplate queryTemplate);

    @IterableMapping(elementTargetType = QueryTemplateDto.class, qualifiedByName = "autoCompleteMapping")
    List<QueryTemplateDto> fromEntityListToQueryTemplateAutoCompleteDto(List<QueryTemplate> queryTemplates);
}
