package log.monitor.api.mapper;

import log.monitor.api.dto.notification.NotificationDto;
import log.monitor.api.model.Notification;
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
        uses = {NotificationGroupMapper.class})
public interface NotificationMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "state", target = "state")
    @Mapping(source = "notificationGroup", target = "notificationGroup", qualifiedByName = "fromEntityToNotificationGroupDtoAutoComplete")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "createdDate", target = "createdDate")
    @Mapping(source = "modifiedDate", target = "modifiedDate")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToNotificationDto")
    NotificationDto fromEntityToNotificationDto(Notification notification);

    @IterableMapping(elementTargetType = NotificationDto.class, qualifiedByName = "fromEntityToNotificationDto")
    List<NotificationDto> fromEntityListToNotificationDtoList(List<Notification> notifications);
}
