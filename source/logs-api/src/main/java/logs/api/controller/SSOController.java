package logs.api.controller;

import logs.api.constant.BaseConstant;
import logs.api.dto.ApiMessageDto;
import logs.api.dto.ErrorCode;
import logs.api.exception.UnauthorizedException;
import logs.api.model.Account;
import logs.api.repository.AccountRepository;
import logs.api.service.feign.FeignSSOService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class SSOController extends ABasicController {
    @Autowired
    private FeignSSOService feignSSOService;

    @Autowired
    private AccountRepository accountRepository;

    @GetMapping(value = "/sso/check-token", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiMessageDto<String> login() {
        Account account = accountRepository.findByIdAndStatus(getCurrentUser(), BaseConstant.STATUS_ACTIVE)
                .orElseThrow(() -> new UnauthorizedException("Account can not be found", ErrorCode.ACCOUNT_ERROR_NOT_FOUND));
        return feignSSOService.verifyToken(BaseConstant.AUTH_BEARER_TOKEN + getCurrentToken());
    }
}