package log.monitor.api.mapper;

import log.monitor.api.dto.notificationGroup.NotificationGroupDto;
import log.monitor.api.form.notificationGroup.CreateNotificationGroupForm;
import log.monitor.api.form.notificationGroup.UpdateNotificationGroupForm;
import log.monitor.api.model.NotificationGroup;
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
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface NotificationGroupMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "channelSetting", target = "channelSetting")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "createdDate", target = "createdDate")
    @Mapping(source = "modifiedDate", target = "modifiedDate")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToNotificationGroupDto")
    NotificationGroupDto fromEntityToNotificationGroupDto(NotificationGroup notificationGroup);

    @IterableMapping(elementTargetType = NotificationGroupDto.class, qualifiedByName = "fromEntityToNotificationGroupDto")
    List<NotificationGroupDto> fromEntityListToNotificationGroupDtoList(List<NotificationGroup> notificationGroups);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "type", target = "type")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToNotificationGroupDtoAutoComplete")
    NotificationGroupDto fromEntityToNotificationGroupDtoAutoComplete(NotificationGroup notificationGroup);

    @IterableMapping(elementTargetType = NotificationGroupDto.class, qualifiedByName = "fromEntityToNotificationGroupDtoAutoComplete")
    List<NotificationGroupDto> fromEntityListToNotificationGroupDtoAutoCompleteList(List<NotificationGroup> notificationGroups);

    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "channelSetting", target = "channelSetting")
    @Mapping(source = "type", target = "type")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromCreateFormToEntity")
    NotificationGroup fromFormToEntity(CreateNotificationGroupForm createNotificationGroupForm);

    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "channelSetting", target = "channelSetting")
    @Mapping(source = "type", target = "type")
    @BeanMapping(ignoreByDefault = true)
    @Named("updateEntityFromForm")
    void updateEntityFromForm(UpdateNotificationGroupForm updateNotificationGroupForm, @MappingTarget NotificationGroup notificationGroup);

    @Mapping(source = "id", target = "id")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToNotificationGroupIdDto")
    NotificationGroupDto fromEntityToNotificationGroupIdDto(NotificationGroup notificationGroup);
}
