package logs.api.mapper;

import logs.api.dto.notificationRule.NotificationRuleItemDto;
import logs.api.model.NotificationRuleItem;
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
        uses = {ApplicationsMapper.class, QueryTemplateMapper.class})
public interface NotificationRuleItemMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "application", target = "application", qualifiedByName = "fromEntityToApplicationShortDto")
    @Mapping(source = "queryTemplate", target = "queryTemplate", qualifiedByName = "fromEntityToQueryTemplateShortDto")
    @Mapping(source = "ordering", target = "ordering")
    @Mapping(source = "operator", target = "operator")
    @Mapping(source = "modifiedDate", target = "modifiedDate")
    @Mapping(source = "createdDate", target = "createdDate")
    @Mapping(source = "status", target = "status")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToNotificationRuleItemDto")
    NotificationRuleItemDto fromEntityToNotificationRuleItemDto(NotificationRuleItem notificationRuleItem);

    @IterableMapping(elementTargetType = NotificationRuleItemDto.class, qualifiedByName = "fromEntityToNotificationRuleItemDto")
    List<NotificationRuleItemDto> fromEntityToNotificationRuleItemDtoList(List<NotificationRuleItem> notificationRuleItems);
}
