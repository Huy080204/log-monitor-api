package logs.api.component;

import logs.api.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SortParameterValidatorTest {

    static class Rule {
    }

    static class Account {
    }

    @Test
    void shouldReturnPropertiesWhenAllSortPropertiesBelongToEntity() {
        SortParameterValidator validator = validator();

        List<String> properties = validator.validate(Rule.class, new String[]{"id,desc", "name", "group.name,ASC"});

        assertThat(properties).containsExactly("id", "name", "group.name");
    }

    @Test
    void shouldRejectWhenSortPropertyBelongsToAnotherEntity() {
        SortParameterValidator validator = validator();

        assertThatThrownBy(() -> validator.validate(Rule.class, new String[]{"email"}))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid sort parameter");
    }

    @Test
    void shouldCheckAgainstAllEntitiesWhenEntityIsUnknown() {
        SortParameterValidator validator = validator();

        List<String> properties = validator.validate(Object.class, new String[]{"email", "name"});

        assertThat(properties).containsExactly("email", "name");
    }

    @Test
    void shouldReturnEmptyWhenSortParameterIsAbsent() {
        SortParameterValidator validator = validator();

        List<String> properties = validator.validate(Rule.class, null);

        assertThat(properties).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenSortValuesAreBlank() {
        SortParameterValidator validator = validator();

        List<String> properties = validator.validate(Rule.class, new String[]{"", " , "});

        assertThat(properties).isEmpty();
    }

    @Test
    void shouldSkipDirectionAndIgnoreCaseKeywordsWhenReadingProperties() {
        SortParameterValidator validator = validator();

        List<String> properties = validator.validate(Rule.class, new String[]{"name,DESC,ignorecase"});

        assertThat(properties).containsExactly("name");
    }

    @Test
    void shouldRejectWhenSortPropertyIsNotWhitelisted() {
        SortParameterValidator validator = validator();

        assertThatThrownBy(() -> validator.validate(Rule.class, new String[]{"id,desc", "aaaa1,asc"}))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid sort parameter");
    }

    @Test
    void shouldRejectWhenMoreOrdersThanAllowed() {
        SortParameterValidator validator = validator();

        assertThatThrownBy(() -> validator.validate(Rule.class, new String[]{"id", "name", "createdDate", "group.name"}))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Too many sort properties, max 3");
    }

    @Test
    void shouldRejectWhenKeywordIsNotInTrailingPosition() {
        SortParameterValidator validator = validator();

        assertThatThrownBy(() -> validator.validate(Rule.class, new String[]{"asc,asc,asc,asc"}))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid sort parameter");
    }

    @Test
    void shouldRejectWhenPropertyHasLeadingWhitespace() {
        SortParameterValidator validator = validator();

        assertThatThrownBy(() -> validator.validate(Rule.class, new String[]{" name,desc"}))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid sort parameter");
    }

    @Test
    void shouldRejectWhenIgnoreCaseIsNotLastToken() {
        SortParameterValidator validator = validator();

        assertThatThrownBy(() -> validator.validate(Rule.class, new String[]{"name,ignorecase,desc"}))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid sort parameter");
    }

    @Test
    void shouldDropDotOnlyTokensWhenReadingProperties() {
        SortParameterValidator validator = validator();

        List<String> properties = validator.validate(Rule.class, new String[]{"name,.,id"});

        assertThat(properties).containsExactly("name", "id");
    }

    @Test
    void shouldDropBlankTokensBeforeReadingKeywordsWhenReadingProperties() {
        SortParameterValidator validator = validator();

        List<String> properties = validator.validate(Rule.class, new String[]{"name,desc, "});

        assertThat(properties).containsExactly("name");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldCollectPathsPerEntityWithOneNestedLevelWhenReadingMetamodel() {
        EntityType account = entity(Account.class, singular("email", null));
        EntityType group = entity(Object.class, singular("name", null), singular("owner", account));
        EntityType rule = entity(Rule.class, singular("id", null), singular("group", group), mock(PluralAttribute.class));
        Metamodel metamodel = mock(Metamodel.class);
        Set entities = new HashSet<>(Arrays.asList(rule, account));
        when(metamodel.getEntities()).thenReturn(entities);

        Map<Class<?>, Set<String>> paths = SortParameterValidator.sortablePathsByEntity(metamodel);

        assertThat(paths).containsOnlyKeys(Rule.class, Account.class);
        assertThat(paths.get(Rule.class)).containsExactlyInAnyOrder("id", "group", "group.name", "group.owner");
        assertThat(paths.get(Account.class)).containsExactly("email");
    }

    private static SortParameterValidator validator() {
        Map<Class<?>, Set<String>> paths = new HashMap<>();
        paths.put(Rule.class, new HashSet<>(Arrays.asList("id", "name", "createdDate", "group.name")));
        paths.put(Account.class, Collections.singleton("email"));
        return new SortParameterValidator(paths, 3);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static EntityType entity(Class<?> javaType, Object... attributes) {
        EntityType entity = mock(EntityType.class);
        when(entity.getJavaType()).thenReturn(javaType);
        Set attributeSet = new HashSet<>(Arrays.asList(attributes));
        when(entity.getAttributes()).thenReturn(attributeSet);
        return entity;
    }

    @SuppressWarnings("rawtypes")
    private static SingularAttribute singular(String name, ManagedType target) {
        SingularAttribute attribute = mock(SingularAttribute.class);
        when(attribute.getName()).thenReturn(name);
        when(attribute.getType()).thenReturn(target);
        return attribute;
    }
}
