package logs.api.mapper;

import logs.api.dto.notificationChannel.NotificationChannelDto;
import logs.api.form.notificationChannel.CreateNotificationChannelForm;
import logs.api.form.notificationChannel.UpdateNotificationChannelForm;
import logs.api.model.NotificationChannel;
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
public interface NotificationChannelMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "channelSetting", target = "channelSetting")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "modifiedDate", target = "modifiedDate")
    @Mapping(source = "createdDate", target = "createdDate")
    @Mapping(source = "status", target = "status")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToNotificationChannelDto")
    NotificationChannelDto fromEntityToNotificationChannelDto(NotificationChannel notificationChannel);

    @IterableMapping(elementTargetType = NotificationChannelDto.class, qualifiedByName = "fromEntityToNotificationChannelDto")
    List<NotificationChannelDto> fromEntityToNotificationChannelDtoList(List<NotificationChannel> notificationChannels);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @BeanMapping(ignoreByDefault = true)
    @Named("autoCompleteMapping")
    NotificationChannelDto fromEntityToNotificationChannelDtoAutoComplete(NotificationChannel notificationChannel);

    @IterableMapping(elementTargetType = NotificationChannelDto.class, qualifiedByName = "autoCompleteMapping")
    List<NotificationChannelDto> fromEntityListToNotificationChannelDtoAutoComplete(List<NotificationChannel> notificationChannels);

    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "channelSetting", target = "channelSetting")
    @Mapping(source = "type", target = "type")
    @BeanMapping(ignoreByDefault = true)
    @Named("adminCreateMapping")
    NotificationChannel fromFormToEntity(CreateNotificationChannelForm createNotificationChannelForm);

    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "channelSetting", target = "channelSetting")
    @Mapping(source = "type", target = "type")
    @BeanMapping(ignoreByDefault = true)
    @Named("adminUpdateMapping")
    void updateEntityFromForm(UpdateNotificationChannelForm updateNotificationChannelForm, @MappingTarget NotificationChannel notificationChannel);

    @Mapping(source = "id", target = "id")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToNotificationChannelIdDto")
    NotificationChannelDto fromEntityToNotificationChannelIdDto(NotificationChannel notificationChannel);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToSimpleNotificationChannel")
    NotificationChannelDto fromEntityToSimpleNotificationChannel(NotificationChannel notificationChannel);
}
