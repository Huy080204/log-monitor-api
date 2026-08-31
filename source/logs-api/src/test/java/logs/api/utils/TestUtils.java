package logs.api.utils;

import logs.api.jwt.UserBaseJwt;

public class TestUtils {

    public static UserBaseJwt jwtWithAccountId(Long accountId) {
        UserBaseJwt jwt = new UserBaseJwt();
        jwt.setAccountId(accountId);
        return jwt;
    }

    public static UserBaseJwt superAdminJwt() {
        UserBaseJwt jwt = new UserBaseJwt();
        jwt.getAttributeDto().setIsSuperAdmin(true);
        return jwt;
    }

    public static UserBaseJwt superAdminJwt(long accountId) {
        UserBaseJwt jwt = new UserBaseJwt();
        jwt.setAccountId(accountId);
        jwt.getAttributeDto().setIsSuperAdmin(true);
        return jwt;
    }

    public static UserBaseJwt notSuperAdminJwt() {
        UserBaseJwt jwt = new UserBaseJwt();
        jwt.getAttributeDto().setIsSuperAdmin(false);
        return jwt;
    }

    public static UserBaseJwt jwtWithUserKind(Integer userKind) {
        UserBaseJwt jwt = new UserBaseJwt();
        jwt.setUserKind(userKind);
        return jwt;
    }
}
