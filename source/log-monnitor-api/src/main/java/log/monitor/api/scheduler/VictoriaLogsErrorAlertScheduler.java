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
import java.util.HashMap;
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
        Map<Long, Integer> countsByQueryId;
        try {
            countsByQueryId = queryCountsByNotificationQuery(query, notificationQueries);
        } catch (Exception e) {
            log.error("Failed to query VictoriaLogs [{}]", query, e);
            return;
        }

        List<NotificationQuery> breaching = new ArrayList<>();
        for (NotificationQuery notificationQuery : notificationQueries) {
            int count = countsByQueryId.getOrDefault(notificationQuery.getId(), 0);
            if (count >= notificationQuery.getCount()) {
                breaching.add(notificationQuery);
            }
        }

        if (breaching.isEmpty()) {
            log.debug("No notification query crossed its threshold in the last {}", BaseConstant.VICTORIALOGS_QUERY_WINDOW);
            return;
        }

        List<String> lines = new ArrayList<>();
        for (NotificationQuery notificationQuery : breaching) {
            int count = countsByQueryId.getOrDefault(notificationQuery.getId(), 0);
            lines.add(String.format(":red_circle: `%s` — %d lỗi trong %s (ngưỡng %d)",
                    notificationQuery.getQuery(), count, BaseConstant.VICTORIALOGS_QUERY_WINDOW, notificationQuery.getCount()));
        }
        Collections.sort(lines);

        String title = String.format("🚨 [%s] %d notification query vượt ngưỡng trong %s",
                activeGroup.getName(), breaching.size(), BaseConstant.VICTORIALOGS_QUERY_WINDOW);
        slackAlertService.sendMessage(title, lines);
    }

    private Map<Long, Integer> queryCountsByNotificationQuery(String query, List<NotificationQuery> notificationQueries) throws Exception {
        Map<Long, Integer> counts = new HashMap<>();
        String rawBody = feignVictoriaLogsService.query(FeignConst.LOGIN_TYPE_NO_AUTH, query);
        if (rawBody == null || rawBody.trim().isEmpty()) {
            return counts;
        }

        MappingIterator<Map<String, String>> lines = objectMapper
                .readerFor(new TypeReference<Map<String, String>>() {})
                .readValues(rawBody);
        if (!lines.hasNext()) {
            return counts;
        }

        Map<String, String> statsLine = lines.next();
        for (NotificationQuery notificationQuery : notificationQueries) {
            String value = statsLine.get(statsAlias(notificationQuery));
            counts.put(notificationQuery.getId(), value == null || value.isEmpty() ? 0 : Integer.parseInt(value));
        }
        return counts;
    }

    /**
     * Batches every active NotificationQuery into a single LogsQL request using conditional
     * stats (`count() if (<filter>)`), so each query's match count is fetched in one call
     * instead of one VictoriaLogs request per query.
     */
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
        return String.format("_time:%s | stats %s", BaseConstant.VICTORIALOGS_QUERY_WINDOW, stats);
    }

    private String statsAlias(NotificationQuery notificationQuery) {
        return STATS_ALIAS_PREFIX + notificationQuery.getId();
    }
}
