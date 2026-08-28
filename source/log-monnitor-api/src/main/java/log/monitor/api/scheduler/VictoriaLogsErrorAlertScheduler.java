package log.monitor.api.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import log.monitor.api.constant.BaseConstant;
import log.monitor.api.model.NotificationGroup;
import log.monitor.api.model.NotificationQuery;
import log.monitor.api.repository.NotificationGroupRepository;
import log.monitor.api.repository.NotificationQueryRepository;
import log.monitor.api.service.SlackAlertService;
import log.monitor.api.service.feign.FeignConst;
import log.monitor.api.service.feign.FeignVictoriaLogsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class VictoriaLogsErrorAlertScheduler {

    private static final String STATS_ALIAS_PREFIX = "q_";

    @Autowired
    private FeignVictoriaLogsService feignVictoriaLogsService;

    @Autowired
    private NotificationGroupRepository notificationGroupRepository;

    @Autowired
    private NotificationQueryRepository notificationQueryRepository;

    @Autowired
    private SlackAlertService slackAlertService;

    @Autowired
    private ObjectMapper objectMapper;

    @Scheduled(cron = "0 */5 * * * *")
    public void checkErrorRateAndAlert() {
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

        String query = buildQuery(notificationQueries);
        Map<String, List<String>> breachLinesByApp;
        try {
            breachLinesByApp = queryBreachesByApp(query, notificationQueries);
        } catch (Exception e) {
            log.error("Failed to query VictoriaLogs [{}]", query, e);
            return;
        }

        if (breachLinesByApp.isEmpty()) {
            log.debug("No app crossed any notification query threshold in the last {}", BaseConstant.VICTORIALOGS_QUERY_WINDOW);
            return;
        }

        List<String> apps = new ArrayList<>(breachLinesByApp.keySet());
        Collections.sort(apps);

        List<String> lines = new ArrayList<>();
        for (String app : apps) {
            lines.add(String.format("*%s*", app));
            lines.addAll(breachLinesByApp.get(app));
        }

        String title = String.format("🚨 [%s] %d app vượt ngưỡng trong %s",
                activeGroup.getName(), apps.size(), BaseConstant.VICTORIALOGS_QUERY_WINDOW);
        slackAlertService.sendMessage(title, lines);
    }

    /**
     * Runs one LogsQL request grouped by application, with a conditional count per
     * NotificationQuery (`count() if (<filter>) as q_<id>`), then keeps only the
     * (app, query) pairs whose count reached that query's own threshold.
     */
    private Map<String, List<String>> queryBreachesByApp(String query, List<NotificationQuery> notificationQueries) throws Exception {
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

    private String buildQuery(List<NotificationQuery> notificationQueries) {
        StringBuilder stats = new StringBuilder();
        for (NotificationQuery notificationQuery : notificationQueries) {
            if (stats.length() > 0) {
                stats.append(", ");
            }
            stats.append("count() if (")
                    .append(notificationQuery.getQuery())
                    .append(") as ")
                    .append(statsAlias(notificationQuery));
        }
        return String.format("_time:%s | stats by (%s) %s",
                BaseConstant.VICTORIALOGS_QUERY_WINDOW, BaseConstant.VICTORIALOGS_QUERY_APP_FIELD, stats);
    }

    private String statsAlias(NotificationQuery notificationQuery) {
        return STATS_ALIAS_PREFIX + notificationQuery.getId();
    }
}
