package log.monitor.api.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

import log.monitor.api.constant.BaseConstant;
import log.monitor.api.dto.ApiMessageDto;
import log.monitor.api.dto.ErrorCode;
import log.monitor.api.exception.UnauthorizedException;
import log.monitor.api.model.Account;
import log.monitor.api.repository.AccountRepository;
import log.monitor.api.service.feign.FeignSSOService;
import log.monitor.api.service.impl.UserServiceImpl;
import log.monitor.api.utils.TestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SSOControllerTest {

    @Mock private FeignSSOService feignSSOService;
    @Mock private AccountRepository accountRepository;
    @Mock private UserServiceImpl userService;
    @InjectMocks private SSOController controller;

    @Test
    void shouldThrowUnauthorizedWhenAccountDoesNotExist() {
        when(userService.getAddInfoFromToken()).thenReturn(TestUtils.jwtWithAccountId(1L));
        when(accountRepository.findByIdAndStatus(1L, BaseConstant.STATUS_ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(controller::login)
                .isInstanceOf(UnauthorizedException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.ACCOUNT_ERROR_NOT_FOUND);
    }

    @Test
    void shouldReturnVerifyTokenResponseWhenAccountExists() {
        Account account = new Account();
        when(userService.getAddInfoFromToken()).thenReturn(TestUtils.jwtWithAccountId(1L));
        when(accountRepository.findByIdAndStatus(1L, BaseConstant.STATUS_ACTIVE)).thenReturn(Optional.of(account));

        Authentication authentication = mock(Authentication.class);
        OAuth2AuthenticationDetails details = mock(OAuth2AuthenticationDetails.class);
        when(authentication.getDetails()).thenReturn(details);
        when(details.getTokenValue()).thenReturn("token-value");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            ApiMessageDto<String> expected = new ApiMessageDto<>();
            when(feignSSOService.verifyToken(BaseConstant.AUTH_BEARER_TOKEN + "token-value")).thenReturn(expected);

            ApiMessageDto<String> result = controller.login();

            assertThat(result).isSameAs(expected);
            verify(feignSSOService).verifyToken(BaseConstant.AUTH_BEARER_TOKEN + "token-value");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
