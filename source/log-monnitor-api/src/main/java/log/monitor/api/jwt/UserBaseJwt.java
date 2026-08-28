package log.monitor.api.jwt;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.map.ObjectMapper;
import log.monitor.api.jwt.dto.AttributeDto;
import log.monitor.api.utils.ZipUtils;

import java.io.Serializable;

@Slf4j
@Data
public class UserBaseJwt implements Serializable {
    public static final String DELIM = "\\|";
    public static final String EMPTY_STRING = "<>";
    private Long tokenId;

    private Long accountId = -1L;
    private String appName = EMPTY_STRING;
    private Long storeId = -1L;
    private String kind = EMPTY_STRING;//token kind
    private String permission = EMPTY_STRING;
    private Long deviceId = -1L;// id cua thiet bi, lưu ở table device để get firebase url..
    private Integer userKind = -1; //loại user là admin hay là gì
    private String username = EMPTY_STRING;// username hoac order code
    private Integer tabletKind = -1;
    private Long orderId = -1L;
    private String attribute = EMPTY_STRING;
    private String tenantId = EMPTY_STRING;

    private AttributeDto attributeDto = new AttributeDto();

    public static UserBaseJwt decode(String input) {
        UserBaseJwt result = null;
        try {
            String[] items = ZipUtils.unzipString(input).split(DELIM, 12);
            if (items.length >= 10) {
                result = new UserBaseJwt();
                result.setAccountId(parserLong(items[0]));
                result.setAppName(checkString(items[1]));
                result.setStoreId(parserLong(items[2]));
                result.setKind(checkString(items[3]));
                result.setPermission(checkString(items[4]));
                result.setDeviceId(parserLong(items[5]));
                result.setUserKind(parserInt(items[6]));
                result.setUsername(checkString(items[7]));
                result.setTabletKind(parserInt(items[8]));
                result.setOrderId(parserLong(items[9]));
                result.setAttribute(checkString(items[10]));
                if (items.length > 12) {
                    result.setTenantId(checkString(items[11]));
                }
                result.setAttributeDto(new ObjectMapper().readValue(result.getAttribute(), AttributeDto.class));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    private static Long parserLong(String input) {
        try {
            Long out = Long.parseLong(input);
            if (out > 0) {
                return out;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private static Integer parserInt(String input) {
        try {
            Integer out = Integer.parseInt(input);
            if (out > 0) {
                return out;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private static String checkString(String input) {
        if (!input.equals(EMPTY_STRING)) {
            return input;
        }
        return null;
    }

}
