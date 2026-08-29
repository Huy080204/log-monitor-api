package log.monitor.api.mapper;

import log.monitor.api.dto.notificationQuery.NotificationQueryDto;
import log.monitor.api.form.notificationQuery.CreateNotificationQueryForm;
import log.monitor.api.form.notificationQuery.UpdateNotificationQueryForm;
import log.monitor.api.model.NotificationQuery;
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
        uses = {NotificationGroupMapper.class})
public interface NotificationQueryMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "query", target = "query")
    @Mapping(source = "count", target = "count")
    @Mapping(source = "notificationGroup", target = "notificationGroup", qualifiedByName = "fromEntityToNotificationGroupDtoAutoComplete")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "createdDate", target = "createdDate")
    @Mapping(source = "modifiedDate", target = "modifiedDate")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToNotificationQueryDto")
    NotificationQueryDto fromEntityToNotificationQueryDto(NotificationQuery notificationQuery);

    @IterableMapping(elementTargetType = NotificationQueryDto.class, qualifiedByName = "fromEntityToNotificationQueryDto")
    List<NotificationQueryDto> fromEntityListToNotificationQueryDtoList(List<NotificationQuery> notificationQueries);

    @Mapping(source = "name", target = "name")
    @Mapping(source = "query", target = "query")
    @Mapping(source = "count", target = "count")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromCreateFormToEntity")
    NotificationQuery fromFormToEntity(CreateNotificationQueryForm createNotificationQueryForm);

    @Mapping(source = "name", target = "name")
    @Mapping(source = "query", target = "query")
    @Mapping(source = "count", target = "count")
    @BeanMapping(ignoreByDefault = true)
    @Named("updateEntityFromForm")
    void updateEntityFromForm(UpdateNotificationQueryForm updateNotificationQueryForm, @MappingTarget NotificationQuery notificationQuery);

    @Mapping(source = "id", target = "id")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToNotificationQueryIdDto")
    NotificationQueryDto fromEntityToNotificationQueryIdDto(NotificationQuery notificationQuery);
}
