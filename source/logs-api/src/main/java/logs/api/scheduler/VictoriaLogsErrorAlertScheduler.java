package logs.api.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import logs.api.constant.BaseConstant;
import logs.api.model.Notification;
import logs.api.model.NotificationGroup;
import logs.api.model.NotificationQuery;
import logs.api.repository.NotificationGroupRepository;
import logs.api.repository.NotificationQueryRepository;
import logs.api.repository.NotificationRepository;
import logs.api.service.feign.FeignConst;
import logs.api.service.feign.FeignVictoriaLogsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class VictoriaLogsErrorAlertScheduler {

    private static final String ELLIPSIS = "...";

    @Autowired
    private FeignVictoriaLogsService feignVictoriaLogsService;

    @Autowired
    private NotificationGroupRepository notificationGroupRepository;

    @Autowired
    private NotificationQueryRepository notificationQueryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Scheduled(cron = "0 */5 * * * *")
    public void checkErrorRateAndAlert() {
        try {
            log.info("Start checkErrorRateAndAlert");
            NotificationGroup activeGroup = notificationGroupRepository.findFirstByStatus(BaseConstant.STATUS_ACTIVE).orElse(null);
            if (activeGroup == null) {
                log.debug("No active notification group configured, skip VictoriaLogs error check");
                return;
            }

            List<NotificationQuery> notificationQueries = notificationQueryRepository
                    .findAllByNotificationGroupIdAndStatus(activeGroup.getId(), BaseConstant.STATUS_ACTIVE);
            if (notificationQueries.isEmpty()) {
                log.debug("Active notification group [{}] has no active notification query, skip VictoriaLogs error check", activeGroup.getName());
                return;
            }

            Map<String, List<String>> breachLinesByApp = queryBreachesByApp(buildQuery(notificationQueries), notificationQueries);
            if (breachLinesByApp.isEmpty()) {
                log.debug("No app crossed any notification query threshold in the last {}", BaseConstant.VICTORIALOGS_QUERY_WINDOW);
                return;
            }

            List<String> apps = new ArrayList<>(breachLinesByApp.keySet());
            Collections.sort(apps);

            String title = "🚨 Cảnh báo hệ thống";
            int budget = messageBudget(activeGroup.getType()) - title.length() - 1;
            if (budget <= 0) {
                log.error("Message limit for channel type [{}] is too small for title [{}], skip creating notification",
                        activeGroup.getType(), title);
                return;
            }

            List<Notification> notifications = new ArrayList<>();
            for (String bodyPart : packBodies(apps, breachLinesByApp, budget)) {
                Notification notification = new Notification();
                notification.setMessage(title + "\n" + bodyPart);
                notification.setState(BaseConstant.NOTIFICATION_STATE_SENT);
                notification.setNotificationGroup(activeGroup);
                notifications.add(notification);
            }
            notificationRepository.saveAll(notifications);
            log.info("Successfully created {} notifications for {} breaching app(s)", notifications.size(), apps.size());
        } catch (Exception e) {
            log.error("Error occurred in checkErrorRateAndAlert schedule", e);
        }
    }

    // Pack app chunks into messages, starting a new message once the limit is hit
    private List<String> packBodies(List<String> apps, Map<String, List<String>> breachLinesByApp, int budget) {
        List<String> bodies = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String app : apps) {
            for (String chunk : buildAppChunks(app, breachLinesByApp.get(app), budget)) {
                if (current.length() > 0 && current.length() + 1 + chunk.length() > budget) {
                    bodies.add(current.toString());
                    current = new StringBuilder();
                }
                if (current.length() > 0) {
                    current.append("\n");
                }
                current.append(chunk);
            }
        }
        if (current.length() > 0) {
            bodies.add(current.toString());
        }
        return bodies;
    }

    // Split one app's breach lines into chunks only if they alone exceed the limit
    private List<String> buildAppChunks(String app, List<String> breachLines, int budget) {
        String header = String.format("*%s*", app);
        int headerLength = header.length();

        List<String> chunks = new ArrayList<>();
        StringBuilder chunk = new StringBuilder(header);
        for (String breachLine : breachLines) {
            String line = truncate(breachLine, budget - headerLength - 1);
            if (chunk.length() > headerLength && chunk.length() + 1 + line.length() > budget) {
                chunks.add(chunk.toString());
                chunk = new StringBuilder(header);
            }
            chunk.append("\n").append(line);
        }
        chunks.add(chunk.toString());
        return chunks;
    }

    // Max characters allowed per channel
    private int messageBudget(Integer channelType) {
        return Objects.equals(channelType, BaseConstant.NOTIFICATION_CHANNEL_TYPE_TELEGRAM)
                ? BaseConstant.NOTIFICATION_MESSAGE_MAX_LENGTH_TELEGRAM
                : BaseConstant.NOTIFICATION_MESSAGE_MAX_LENGTH_SLACK;
    }

    private String truncate(String value, int maxLength) {
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return maxLength <= ELLIPSIS.length() ? value.substring(0, maxLength)
                : value.substring(0, maxLength - ELLIPSIS.length()) + ELLIPSIS;
    }

    // Run the query, then keep only (app, query) pairs whose count reached that query's threshold
    private Map<String, List<String>> queryBreachesByApp(String query, List<NotificationQuery> notificationQueries) throws Exception {
        log.info("Querying VictoriaLogs for breaches with query [{}]", query);
        Map<String, List<String>> breachLinesByApp = new LinkedHashMap<>();
        String rawBody = feignVictoriaLogsService.query(FeignConst.LOGIN_TYPE_NO_AUTH, query);
        if (rawBody == null || rawBody.trim().isEmpty()) {
            return breachLinesByApp;
        }

        MappingIterator<Map<String, String>> lines = objectMapper
                .readerFor(new TypeReference<Map<String, String>>() {})
                .readValues(rawBody);
        while (lines.hasNext()) {
            Map<String, String> line = lines.next();
            String app = line.getOrDefault(BaseConstant.VICTORIALOGS_QUERY_APP_FIELD, "unknown");
            for (NotificationQuery notificationQuery : notificationQueries) {
                String value = line.get(statsAlias(notificationQuery));
                int count = value == null || value.isEmpty() ? 0 : Integer.parseInt(value);
                if (count >= notificationQuery.getCount()) {
                    breachLinesByApp.computeIfAbsent(app, k -> new ArrayList<>())
                            .add(String.format("  • `%s`: %d", notificationQuery.getQuery(), count));
                }
            }
        }
        return breachLinesByApp;
    }

    // Build LogsQL: time window + group by app + one conditional count("count() if (_msg:<phrase>) as "<id>"") per query
    private String buildQuery(List<NotificationQuery> notificationQueries) {
        StringBuilder stats = new StringBuilder();
        for (NotificationQuery notificationQuery : notificationQueries) {
            String phrase = notificationQuery.getQuery();
            if (phrase == null || phrase.trim().isEmpty()) {
                log.warn("NotificationQuery [{}] has a blank query, skip it", notificationQuery.getId());
                continue;
            }
            if (stats.length() > 0) {
                stats.append(", ");
            }
            stats.append("count() if (")
                    .append(BaseConstant.VICTORIALOGS_QUERY_MESSAGE_FIELD)
                    .append(":")
                    .append(phrase.trim())
                    .append(") as \"")
                    .append(statsAlias(notificationQuery))
                    .append("\"");
        }
        return String.format("_time:%s | stats by (%s) %s",
                BaseConstant.VICTORIALOGS_QUERY_WINDOW, BaseConstant.VICTORIALOGS_QUERY_APP_FIELD, stats);
    }

    private String statsAlias(NotificationQuery notificationQuery) {
        return String.valueOf(notificationQuery.getId());
    }
}
