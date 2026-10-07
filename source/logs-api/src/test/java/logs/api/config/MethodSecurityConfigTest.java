package logs.api.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodSecurityConfigTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAllowHasRoleWhenTokenAuthorityHasRolePrefix() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(MethodSecurityConfig.class, SecuredService.class)) {
            SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", null, "ROLE_NOG_L"));
            SecuredService service = context.getBean(SecuredService.class);

            String result = service.list();

            assertThat(result).isEqualTo("ok");
        }
    }

    @Test
    void shouldDenyHasRoleWhenTokenLacksAuthority() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(MethodSecurityConfig.class, SecuredService.class)) {
            SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", null, "ROLE_NOG_V"));
            SecuredService service = context.getBean(SecuredService.class);

            assertThatThrownBy(service::list).isInstanceOf(AccessDeniedException.class);
        }
    }

    static class SecuredService {
        @PreAuthorize("hasRole('NOG_L')")
        public String list() {
            return "ok";
        }
    }
}
