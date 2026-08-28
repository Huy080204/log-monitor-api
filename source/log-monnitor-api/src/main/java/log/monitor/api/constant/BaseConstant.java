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

    // NotificationGroup channel type
    public static final Integer NOTIFICATION_CHANNEL_TYPE_TELEGRAM = 0;
    public static final Integer NOTIFICATION_CHANNEL_TYPE_SLACK = 1;

    // Notification state
    public static final Integer NOTIFICATION_STATE_SENT = 0;
    public static final Integer NOTIFICATION_STATE_READ = 1;

    // VictoriaLogs error-alert scheduler query config
    public static final String VICTORIALOGS_QUERY_WINDOW = "5m";
    public static final String VICTORIALOGS_QUERY_APP_FIELD = "application";

    private BaseConstant() {
        throw new IllegalStateException("Utility class");
    }
}
