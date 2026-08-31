package logs.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
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

import javax.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/account")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class AccountController extends ABasicController {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountMapper accountMapper;

    @Value("${sso.app.secretId}")
    private String secretId;

    @Autowired
    private UserServiceImpl userService;

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ACC_L')")
    public ApiMessageDto<ResponseListDto<List<AccountDto>>> list(AccountCriteria accountCriteria, Pageable pageable) {
        // set default filter status = active
        if (accountCriteria.getStatus() == null) {
            accountCriteria.setStatus(BaseConstant.STATUS_ACTIVE);
        }
        Page<Account> careerList = accountRepository.findAll(accountCriteria.getSpecification(), pageable);
        return makeSuccessResponse(makeResponseListDto(careerList, accountMapper::convertAccountToListShortDto), "Get account list success");
    }

    @GetMapping(value = "/auto-complete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<ResponseListDto<List<AccountDto>>> autoComplete(AccountCriteria accountCriteria, Pageable pageable) {
        accountCriteria.setStatus(BaseConstant.STATUS_ACTIVE);
        Page<Account> accounts = accountRepository.findAll(accountCriteria.getSpecification(), pageable);
        return makeSuccessResponse(makeResponseListDto(accounts, accountMapper::convertAccountToAutoCompleteDto), "List account success");
    }

    @GetMapping(value = "/get/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ACC_V')")
    public ApiMessageDto<AccountDto> get(@PathVariable Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Can not found account", ErrorCode.ACCOUNT_ERROR_NOT_FOUND));
        return makeSuccessResponse(accountMapper.fromAccountToShortDto(account), "Get account success");
    }

    @GetMapping(value = "/profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<AccountDto> profile() {
        Account account = accountRepository.findByIdAndStatus(getCurrentUser(), BaseConstant.STATUS_ACTIVE)
                .orElseThrow(() -> new NotFoundException("Not found account", ErrorCode.ACCOUNT_ERROR_NOT_FOUND));

        AccountDto accountDto = accountMapper.fromAccountToDtoSSO(account);
        accountDto.setPermissions(userService.getAuthorities());
        return makeSuccessResponse(accountDto, "Get account success");
    }

    @Hidden
    @PostMapping(value = "/synchronize", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<Void> synchronizeAccounts(@Valid @RequestBody AccountAppsDto accountAppsDto, BindingResult bindingResult) {
        if (!accountAppsDto.getSecretIdApp().equals(secretId)) {
            throw new BadRequestException("Invalid secret ID", ErrorCode.ACCOUNT_ERROR_INVALID_SECRET_ID);
        }
        Set<Long> idsFromSSO = accountAppsDto.getAccountApps().stream()
                .map(SSOAccountDto::getId)
                .collect(Collectors.toSet());
        log.debug(idsFromSSO.toString());

        // update status delete to account not in data sso synchronize
        accountRepository.updateStatusNotInId(BaseConstant.STATUS_DELETE, idsFromSSO);

        // save account
        List<Account> accountsFromSSO = accountMapper.fromAccountDtoListToEntityList(accountAppsDto.getAccountApps());
        accountRepository.saveAll(accountsFromSSO);
        return makeSuccessResponse("Synchronization successful");
    }

    @DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ACC_D')")
    public ApiMessageDto<Void> delete(@PathVariable Long id) {
        // only delete account status delete
        Account account = accountRepository.findByIdAndStatus(id, BaseConstant.STATUS_DELETE)
                .orElseThrow(() -> new NotFoundException("Can not found account", ErrorCode.ACCOUNT_ERROR_NOT_FOUND));

        // delete account
        accountRepository.deleteById(account.getId());
        return makeSuccessResponse("Delete account success");
    }
}
