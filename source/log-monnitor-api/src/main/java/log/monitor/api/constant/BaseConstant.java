package log.monitor.api.constant;

public class BaseConstant {
    public static final String DATE_FORMAT = "dd/MM/yyyy";
    public static final String DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm:ss";
    public static final String LOGIN_TYPE_INTERNAL = "LOGIN_TYPE_INTERNAL";
    public static final String AUTH_BEARER_TOKEN = "Bearer ";
    public static final String DELIM = "::";

    public static final Integer USER_KIND_ADMIN = 1;

    public static final Integer STATUS_ACTIVE = 1;
    public static final Integer STATUS_PENDING = 0;
    public static final Integer STATUS_LOCK = -1;
    public static final Integer STATUS_DELETE = -2;

    public static final String SUCCESS = "SUCCESS";

    public static final String HEADER_CLIENT_TYPE = "X-Client-Type";
    public static final String HEADER_CLIENT_TYPE_WEB = "WEB";

    // Setting keys used by VictoriaLogs error-alert scheduler to resolve Slack bot token/channel
    public static final String SETTING_GROUP_NOTIFICATION = "notification";
    public static final String SETTING_KEY_SLACK_ERROR_ALERT = "slack_error_alert";

    // VictoriaLogs error-alert scheduler query config
    public static final String VICTORIALOGS_QUERY_APP_FIELD = "application";
    public static final String VICTORIALOGS_QUERY_WINDOW = "5m";
    public static final String VICTORIALOGS_ERROR_FIELD = "_msg";
    public static final String VICTORIALOGS_ERROR_VALUE = "ERROR";
    public static final Integer VICTORIALOGS_ERROR_THRESHOLD = 25;

    private BaseConstant() {
        throw new IllegalStateException("Utility class");
    }
}
