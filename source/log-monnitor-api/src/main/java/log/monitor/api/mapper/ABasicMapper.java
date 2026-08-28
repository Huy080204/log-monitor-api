package log.monitor.api.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import log.monitor.api.dto.account.SSOAccountDto;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ABasicMapper {
    ObjectMapper objectMapper = new ObjectMapper();

    @Named("mapAttributeToIsSuperAdmin")
    default boolean mapAttributeToIsSuperAdmin(SSOAccountDto account) {
        try {
            JsonNode attributeNode = objectMapper.readTree(account.getAttribute());
            return attributeNode.has("isSuperAdmin") && attributeNode.get("isSuperAdmin").asBoolean();
        } catch (Exception e) {
            return false;
        }
    }
}