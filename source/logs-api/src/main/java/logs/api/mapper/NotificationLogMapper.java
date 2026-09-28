package logs.api.mapper;

import logs.api.dto.notificationLog.NotificationLogDto;
import logs.api.model.NotificationLog;
import org.mapstruct.BeanMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface NotificationLogMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "appId", target = "appId")
    @Mapping(source = "appName", target = "appName")
    @Mapping(source = "errorName", target = "errorName")
    @Mapping(source = "link", target = "link")
    @Mapping(source = "modifiedDate", target = "modifiedDate")
    @Mapping(source = "createdDate", target = "createdDate")
    @Mapping(source = "status", target = "status")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToNotificationLogDto")
    NotificationLogDto fromEntityToNotificationLogDto(NotificationLog notificationLog);

    @IterableMapping(elementTargetType = NotificationLogDto.class, qualifiedByName = "fromEntityToNotificationLogDto")
    List<NotificationLogDto> fromEntityToNotificationLogDtoList(List<NotificationLog> notificationLogs);
}
