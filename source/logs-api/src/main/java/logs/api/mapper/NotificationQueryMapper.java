package logs.api.mapper;

import logs.api.dto.notificationQuery.NotificationQueryDto;
import logs.api.model.NotificationQuery;
import org.mapstruct.BeanMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {NotificationGroupMapper.class, QueryTemplateMapper.class})
public interface NotificationQueryMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "notificationGroup", target = "notificationGroup", qualifiedByName = "fromEntityToNotificationGroupDtoAutoComplete")
    @Mapping(source = "queryTemplate", target = "queryTemplate", qualifiedByName = "fromEntityToQueryTemplateDto")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "createdDate", target = "createdDate")
    @Mapping(source = "modifiedDate", target = "modifiedDate")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToNotificationQueryDto")
    NotificationQueryDto fromEntityToNotificationQueryDto(NotificationQuery notificationQuery);

    @IterableMapping(elementTargetType = NotificationQueryDto.class, qualifiedByName = "fromEntityToNotificationQueryDto")
    List<NotificationQueryDto> fromEntityListToNotificationQueryDtoList(List<NotificationQuery> notificationQueries);
}
