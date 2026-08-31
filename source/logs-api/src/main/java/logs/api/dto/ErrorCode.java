package logs.api.dto;

public class ErrorCode {
    /**
     * General error code
     */
    public static final String GENERAL_ERROR_REQUIRE_PARAMS = "ERROR-GENERAL-0000";
    public static final String GENERAL_ERROR_STORE_LOCKED = "ERROR-GENERAL-0001";
    public static final String GENERAL_ERROR_ACCOUNT_LOCKED = "ERROR-GENERAL-0002";
    public static final String GENERAL_ERROR_SHOP_LOCKED = "ERROR-GENERAL-0003";
    public static final String GENERAL_ERROR_STORE_NOT_FOUND = "ERROR-GENERAL-0004";
    public static final String GENERAL_ERROR_ACCOUNT_NOT_FOUND = "ERROR-GENERAL-0005";

    /**
     * Starting error code Account
     */
    public static final String ACCOUNT_ERROR_NOT_FOUND = "ERROR-ACCOUNT-0000";
    public static final String ACCOUNT_ERROR_INVALID_SECRET_ID = "ERROR-ACCOUNT-0001";

    /**
     * Starting error code Setting
     */
    public static final String SETTING_ERROR_NOT_FOUND = "ERROR-SETTING-0000";
    public static final String SETTING_ERROR_EXISTED_GROUP_NAME_AND_KEY_NAME = "ERROR-SETTING-0001";

    /**
     * Starting error code NotificationGroup
     */
    public static final String NOTIFICATION_GROUP_ERROR_NOT_FOUND = "ERROR-NOTIFICATION-GROUP-0000";
    public static final String NOTIFICATION_GROUP_ERROR_NAME_EXISTED = "ERROR-NOTIFICATION-GROUP-0001";
    public static final String NOTIFICATION_GROUP_ERROR_DELETE_ACTIVE = "ERROR-NOTIFICATION-GROUP-0002";

    /**
     * Starting error code Notification
     */
    public static final String NOTIFICATION_ERROR_NOT_FOUND = "ERROR-NOTIFICATION-0000";

    /**
     * Starting error code NotificationQuery
     */
    public static final String NOTIFICATION_QUERY_ERROR_NOT_FOUND = "ERROR-NOTIFICATION-QUERY-0000";
    public static final String NOTIFICATION_QUERY_ERROR_EXISTED = "ERROR-NOTIFICATION-QUERY-0001";
    public static final String NOTIFICATION_QUERY_ERROR_NAME_EXISTED = "ERROR-NOTIFICATION-QUERY-0002";

    /**
     * Starting error code DATABASE_ERROR
     */
    public static final String ERROR_DB_QUERY = "ERROR-DB-QUERY-0000";
}
