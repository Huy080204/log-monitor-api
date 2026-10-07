package logs.api.component;

import logs.api.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;

import jakarta.persistence.Entity;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortParameterInterceptorTest {

    static class Notification {
    }

    static class Account {
    }

    static class NotificationCriteria {
        public Specification<Notification> getCriteria() {
            return null;
        }
    }

    static class AccountCriteria {
        public Specification<Account> getSpecification() {
            return null;
        }
    }

    static class SampleController {
        public void list(NotificationCriteria criteria, Pageable pageable) {
        }

        public void accounts(AccountCriteria criteria, Pageable pageable) {
        }

        public void noCriteria(Pageable pageable) {
        }

        public void get(Long id) {
        }
    }

    @Test
    void shouldRejectWhenSortPropertyBelongsToAnotherEntity() throws Exception {
        SortParameterInterceptor interceptor = interceptor();
        MockHttpServletRequest request = request("email");
        HandlerMethod handler = handler("list", NotificationCriteria.class, Pageable.class);

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), handler))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldContinueWhenSortPropertyBelongsToCriteriaEntity() throws Exception {
        SortParameterInterceptor interceptor = interceptor();

        boolean proceed = interceptor.preHandle(request("id,desc"), new MockHttpServletResponse(),
                handler("list", NotificationCriteria.class, Pageable.class));

        assertThat(proceed).isTrue();
    }

    @Test
    void shouldRejectOtherEntityPropertyWhenCriteriaUsesGetSpecification() throws Exception {
        SortParameterInterceptor interceptor = interceptor();
        MockHttpServletRequest request = request("id");
        HandlerMethod handler = handler("accounts", AccountCriteria.class, Pageable.class);

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), handler))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldCheckAgainstAllEntitiesWhenHandlerHasNoCriteria() throws Exception {
        SortParameterInterceptor interceptor = interceptor();

        boolean proceed = interceptor.preHandle(request("email"), new MockHttpServletResponse(),
                handler("noCriteria", Pageable.class));

        assertThat(proceed).isTrue();
    }

    @Test
    void shouldContinueWithoutValidationWhenHandlerTakesNoPageableOrSort() throws Exception {
        SortParameterInterceptor interceptor = interceptor();

        boolean proceed = interceptor.preHandle(request("notAField"), new MockHttpServletResponse(),
                handler("get", Long.class));

        assertThat(proceed).isTrue();
    }

    @Test
    void shouldContinueWhenHandlerIsNotAControllerMethod() {
        SortParameterInterceptor interceptor = interceptor();

        boolean proceed = interceptor.preHandle(request("notAField"), new MockHttpServletResponse(), new Object());

        assertThat(proceed).isTrue();
    }

    @Test
    void shouldResolveAnEntityForEveryPageableEndpointWhenScanningRealControllers() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<String> checked = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("logs.api.controller")) {
            for (Method method : Class.forName(definition.getBeanClassName()).getDeclaredMethods()) {
                if (AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)
                        && Arrays.stream(method.getParameterTypes()).anyMatch(Pageable.class::isAssignableFrom)) {
                    checked.add(method.getName());
                    Class<?> entity = SortParameterInterceptor.sortedEntity(method);
                    if (!entity.isAnnotationPresent(Entity.class)) {
                        unresolved.add(method.getDeclaringClass().getSimpleName() + "#" + method.getName());
                    }
                }
            }
        }

        assertThat(checked).isNotEmpty();
        assertThat(unresolved).isEmpty();
    }

    private static SortParameterInterceptor interceptor() {
        Map<Class<?>, Set<String>> paths = new HashMap<>();
        paths.put(Notification.class, Collections.singleton("id"));
        paths.put(Account.class, Collections.singleton("email"));
        SortParameterInterceptor interceptor = new SortParameterInterceptor();
        ReflectionTestUtils.setField(interceptor, "validator", new SortParameterValidator(paths, 3));
        return interceptor;
    }

    private static MockHttpServletRequest request(String sort) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/x/list");
        request.addParameter("sort", sort);
        return request;
    }

    private static HandlerMethod handler(String method, Class<?>... parameterTypes) throws NoSuchMethodException {
        return new HandlerMethod(new SampleController(), SampleController.class.getMethod(method, parameterTypes));
    }
}
