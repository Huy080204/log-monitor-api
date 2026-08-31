package logs.api.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.dto.ResponseListDto;
import logs.api.dto.account.AccountAppsDto;
import logs.api.dto.account.AccountDto;
import logs.api.dto.account.SSOAccountDto;
import logs.api.exception.BadRequestException;
import logs.api.exception.NotFoundException;
import logs.api.mapper.AccountMapper;
import logs.api.model.Account;
import logs.api.model.criteria.AccountCriteria;
import logs.api.repository.AccountRepository;
import logs.api.service.impl.UserServiceImpl;
import logs.api.utils.TestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock private AccountRepository accountRepository;
    @Mock private AccountMapper accountMapper;
    @Mock private UserServiceImpl userService;
    @InjectMocks private AccountController controller;

    @Test
    void shouldReturnAccountListWhenListCalled() {
        AccountCriteria criteria = new AccountCriteria();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Account> page = new PageImpl<>(List.of(new Account()));
        List<AccountDto> dtoList = List.of(new AccountDto());
        when(accountRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(accountMapper.convertAccountToListShortDto(anyList())).thenReturn(dtoList);

        ApiMessageDto<ResponseListDto<List<AccountDto>>> result = controller.list(criteria, pageable);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData().getContent()).isSameAs(dtoList);
        assertThat(result.getMessage()).isEqualTo("Get account list success");
        assertThat(criteria.getStatus()).isEqualTo(BaseConstant.STATUS_ACTIVE);
    }

    @Test
    void shouldReturnAccountAutoCompleteListWhenAutoCompleteCalled() {
        AccountCriteria criteria = new AccountCriteria();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Account> page = new PageImpl<>(List.of(new Account()));
        List<AccountDto> dtoList = List.of(new AccountDto());
        when(accountRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(accountMapper.convertAccountToAutoCompleteDto(anyList())).thenReturn(dtoList);

        ApiMessageDto<ResponseListDto<List<AccountDto>>> result = controller.autoComplete(criteria, pageable);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData().getContent()).isSameAs(dtoList);
        assertThat(result.getMessage()).isEqualTo("List account success");
        assertThat(criteria.getStatus()).isEqualTo(BaseConstant.STATUS_ACTIVE);
    }

    @Test
    void shouldThrowNotFoundWhenAccountIdDoesNotExist() {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.get(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.ACCOUNT_ERROR_NOT_FOUND);
    }

    @Test
    void shouldReturnAccountDtoWhenIdExists() {
        Account account = new Account();
        AccountDto dto = new AccountDto();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountMapper.fromAccountToShortDto(account)).thenReturn(dto);

        ApiMessageDto<AccountDto> result = controller.get(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(result.getMessage()).isEqualTo("Get account success");
        verify(accountRepository).findById(1L);
    }

    @Test
    void shouldThrowNotFoundWhenProfileAccountDoesNotExist() {
        when(userService.getAddInfoFromToken()).thenReturn(TestUtils.jwtWithAccountId(1L));
        when(accountRepository.findByIdAndStatus(1L, BaseConstant.STATUS_ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(controller::profile)
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.ACCOUNT_ERROR_NOT_FOUND);
    }

    @Test
    void shouldReturnAccountDtoWhenProfileAccountExists() {
        Account account = new Account();
        AccountDto dto = new AccountDto();
        when(userService.getAddInfoFromToken()).thenReturn(TestUtils.jwtWithAccountId(1L));
        when(accountRepository.findByIdAndStatus(1L, BaseConstant.STATUS_ACTIVE)).thenReturn(Optional.of(account));
        when(accountMapper.fromAccountToDtoSSO(account)).thenReturn(dto);

        ApiMessageDto<AccountDto> result = controller.profile();

        assertThat(result.getResult()).isTrue();
        assertThat(result.getData()).isSameAs(dto);
        assertThat(result.getMessage()).isEqualTo("Get account success");
        verify(userService).getAuthorities();
    }

    @Test
    void shouldThrowBadRequestWhenSynchronizeAccountsInvalidSecretId() {
        AccountAppsDto accountAppsDto = new AccountAppsDto();
        accountAppsDto.setSecretIdApp("wrong-secret");

        assertThatThrownBy(() -> controller.synchronizeAccounts(accountAppsDto, null))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.ACCOUNT_ERROR_INVALID_SECRET_ID);
    }

    @Test
    void shouldSynchronizeAccountsWhenSecretIdValid() {
        ReflectionTestUtils.setField(controller, "secretId", "valid-secret");
        SSOAccountDto ssoAccountDto = new SSOAccountDto();
        ssoAccountDto.setId(1L);
        AccountAppsDto accountAppsDto = new AccountAppsDto();
        accountAppsDto.setSecretIdApp("valid-secret");
        accountAppsDto.setAccountApps(List.of(ssoAccountDto));
        List<Account> accountsFromSSO = List.of(new Account());
        when(accountMapper.fromAccountDtoListToEntityList(accountAppsDto.getAccountApps())).thenReturn(accountsFromSSO);

        ApiMessageDto<Void> result = controller.synchronizeAccounts(accountAppsDto, null);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Synchronization successful");
        verify(accountRepository).updateStatusNotInId(BaseConstant.STATUS_DELETE, Set.of(1L));
        verify(accountRepository).saveAll(accountsFromSSO);
    }

    @Test
    void shouldThrowNotFoundWhenDeleteAccountIdDoesNotExist() {
        when(accountRepository.findByIdAndStatus(1L, BaseConstant.STATUS_DELETE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.delete(1L))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.ACCOUNT_ERROR_NOT_FOUND);
    }

    @Test
    void shouldDeleteAccountWhenIdExistsAndStatusDelete() {
        Account account = new Account();
        account.setId(1L);
        when(accountRepository.findByIdAndStatus(1L, BaseConstant.STATUS_DELETE)).thenReturn(Optional.of(account));

        ApiMessageDto<Void> result = controller.delete(1L);

        assertThat(result.getResult()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Delete account success");
        verify(accountRepository).deleteById(1L);
    }
}
