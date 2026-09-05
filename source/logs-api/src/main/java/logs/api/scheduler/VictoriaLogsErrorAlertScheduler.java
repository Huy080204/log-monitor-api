package logs.api.scheduler;

import logs.api.constant.BaseConstant;
import logs.api.dto.victorialogs.VictoriaLogsQueryForm;
import logs.api.dto.victorialogs.VictoriaLogsStatsDto;
import logs.api.model.*;
import logs.api.repository.NotificationGroupRepository;
import logs.api.repository.NotificationQueryRepository;
import logs.api.repository.NotificationRepository;
import logs.api.service.feign.FeignConst;
import logs.api.service.feign.FeignVictoriaLogsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

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

    @Scheduled(cron = "0 */5 * * * *")
    public void checkErrorRateAndAlert() {
        try {
            log.info("Start checkErrorRateAndAlert");
            NotificationGroup activeGroup = notificationGroupRepository.findFirstByStatus(BaseConstant.STATUS_ACTIVE).orElse(null);
            if (activeGroup == null) {
                log.debug("No active notification group configured, skip VictoriaLogs error check");
                return;
            }

            List<NotificationQuery> notificationQueries = notificationQueryRepository.findAllByNotificationGroupId(activeGroup.getId());
            if (notificationQueries.isEmpty()) {
                log.debug("Active notification group [{}] has no active notification query, skip VictoriaLogs error check", activeGroup.getName());
                return;
            }

            Map<Long, QueryTemplate> queryTemplateById = new LinkedHashMap<>();
            Map<String, Set<Long>> enabledTemplateIdsByApp = new LinkedHashMap<>();
            Map<String, String> appNameByVictoriaAppId = new LinkedHashMap<>();
            // dedup into distinct QueryTemplates and which (victoriaAppId, queryTemplate) pairs are enabled
            for (NotificationQuery notificationQuery : notificationQueries) {
                Applications application = notificationQuery.getApplication();
                QueryTemplate queryTemplate = notificationQuery.getQueryTemplate();
                queryTemplateById.putIfAbsent(queryTemplate.getId(), queryTemplate);
                enabledTemplateIdsByApp.computeIfAbsent(application.getVictoriaAppId(), k -> new LinkedHashSet<>())
                        .add(queryTemplate.getId());
                appNameByVictoriaAppId.putIfAbsent(application.getVictoriaAppId(), application.getName());
            }
            if (enabledTemplateIdsByApp.isEmpty()) {
                log.debug("Active notification group [{}] has no usable notification query, skip VictoriaLogs error check", activeGroup.getName());
                return;
            }

            String query = buildQuery(activeGroup.getTimeFrame(), enabledTemplateIdsByApp.keySet(), queryTemplateById.values());
            Map<String, List<String>> breachLinesByApp = queryBreachesByApp(query, queryTemplateById,
                    enabledTemplateIdsByApp, appNameByVictoriaAppId);
            if (breachLinesByApp.isEmpty()) {
                log.debug("No app crossed any notification query threshold in the last {}m", activeGroup.getTimeFrame());
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

    // Run the query, then keep only (app, template) pairs enabled in enabledTemplateIdsByApp whose count hit the threshold
    Map<String, List<String>> queryBreachesByApp(String query, Map<Long, QueryTemplate> queryTemplateById,
                                                 Map<String, Set<Long>> enabledTemplateIdsByApp,
                                                 Map<String, String> appNameByVictoriaAppId) {
        log.info("Querying VictoriaLogs for breaches with query [{}]", query);
        Map<String, List<String>> breachLinesByApp = new LinkedHashMap<>();
        List<VictoriaLogsStatsDto> rows = feignVictoriaLogsService.query(
                FeignConst.LOGIN_TYPE_NO_AUTH, VictoriaLogsQueryForm.of(query));
        if (rows == null || rows.isEmpty()) {
            return breachLinesByApp;
        }

        for (VictoriaLogsStatsDto row : rows) {
            Set<Long> enabledTemplateIds = enabledTemplateIdsByApp.get(row.getApplication());
            if (enabledTemplateIds == null) {
                continue;
            }
            String appName = appNameByVictoriaAppId.getOrDefault(row.getApplication(), row.getApplication());
            for (Long templateId : enabledTemplateIds) {
                QueryTemplate queryTemplate = queryTemplateById.get(templateId);
                int count = row.count(String.valueOf(templateId));
                if (count >= queryTemplate.getCount()) {
                    breachLinesByApp.computeIfAbsent(appName, k -> new ArrayList<>())
                            .add(String.format("  • `%s`: %d", queryTemplate.getName(), count));
                }
            }
        }
        return breachLinesByApp;
    }

    // Build LogsQL: time window + application:in(...) + one count() if per distinct QueryTemplate
    String buildQuery(Integer timeFrameMinutes, Collection<String> victoriaAppIds, Collection<QueryTemplate> queryTemplates) {
        StringBuilder apps = new StringBuilder();
        for (String victoriaAppId : victoriaAppIds) {
            if (apps.length() > 0) {
                apps.append(", ");
            }
            apps.append("\"").append(victoriaAppId).append("\"");
        }

        StringBuilder stats = new StringBuilder();
        for (QueryTemplate queryTemplate : queryTemplates) {
            if (stats.length() > 0) {
                stats.append(", ");
            }
            stats.append("count() if (")
                    .append(queryTemplate.getQuery().trim())
                    .append(") as \"")
                    .append(queryTemplate.getId())
                    .append("\"");
        }

        return String.format("_time:%dm %s:in(%s) | stats by (%s) %s",
                timeFrameMinutes, BaseConstant.VICTORIALOGS_QUERY_APP_FIELD, apps,
                BaseConstant.VICTORIALOGS_QUERY_APP_FIELD, stats);
    }
}
