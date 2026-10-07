package logs.api.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.exception.UnauthorizedException;
import logs.api.model.Account;
import logs.api.repository.AccountRepository;
import logs.api.service.feign.FeignSSOService;
import logs.api.service.impl.UserServiceImpl;
import logs.api.utils.TestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        when(userService.getCurrentToken()).thenReturn("token-value");
        ApiMessageDto<String> expected = new ApiMessageDto<>();
        when(feignSSOService.verifyToken(BaseConstant.AUTH_BEARER_TOKEN + "token-value")).thenReturn(expected);

        ApiMessageDto<String> result = controller.login();

        assertThat(result).isSameAs(expected);
        verify(feignSSOService).verifyToken(BaseConstant.AUTH_BEARER_TOKEN + "token-value");
    }
}
