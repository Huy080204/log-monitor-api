package logs.api.mapper;

import logs.api.dto.notificationRule.NotificationRuleDto;
import logs.api.form.notificationRule.CreateNotificationRuleForm;
import logs.api.form.notificationRule.UpdateNotificationRuleForm;
import logs.api.model.NotificationRule;
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
public interface NotificationRuleMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "notificationGroup", target = "notificationGroup", qualifiedByName = "fromEntityToNotificationGroupDto")
    @Mapping(source = "modifiedDate", target = "modifiedDate")
    @Mapping(source = "createdDate", target = "createdDate")
    @Mapping(source = "status", target = "status")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToNotificationRuleDto")
    NotificationRuleDto fromEntityToNotificationRuleDto(NotificationRule notificationRule);

    @IterableMapping(elementTargetType = NotificationRuleDto.class, qualifiedByName = "fromEntityToNotificationRuleDto")
    List<NotificationRuleDto> fromEntityToNotificationRuleDtoList(List<NotificationRule> notificationRules);

    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromCreateFormToEntity")
    NotificationRule fromFormToEntity(CreateNotificationRuleForm createNotificationRuleForm);

    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @BeanMapping(ignoreByDefault = true)
    @Named("updateEntityFromForm")
    void updateEntityFromForm(UpdateNotificationRuleForm updateNotificationRuleForm, @MappingTarget NotificationRule notificationRule);

    @Mapping(source = "id", target = "id")
    @BeanMapping(ignoreByDefault = true)
    @Named("fromEntityToNotificationRuleIdDto")
    NotificationRuleDto fromEntityToNotificationRuleIdDto(NotificationRule notificationRule);
}
