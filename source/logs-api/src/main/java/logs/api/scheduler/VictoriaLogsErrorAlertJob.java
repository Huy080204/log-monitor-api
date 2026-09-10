package logs.api.scheduler;

import logs.api.constant.BaseConstant;
import logs.api.dto.victorialogs.VictoriaLogsQueryForm;
import logs.api.dto.victorialogs.VictoriaLogsStatsDto;
import logs.api.model.*;
import logs.api.repository.NotificationGroupRepository;
import logs.api.repository.NotificationQueryRepository;
import logs.api.repository.NotificationRepository;
import logs.api.repository.NotificationRuleItemRepository;
import logs.api.repository.NotificationRuleRepository;
import logs.api.service.feign.FeignConst;
import logs.api.service.feign.FeignVictoriaLogsService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@DisallowConcurrentExecution // Prevents concurrent execution of the same notification group's job
@Slf4j
public class VictoriaLogsErrorAlertJob implements Job {

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
    private NotificationRuleRepository notificationRuleRepository;

    @Autowired
    private NotificationRuleItemRepository notificationRuleItemRepository;

    @Override
    public void execute(JobExecutionContext context) {
        Long groupId = (Long) context.getJobDetail().getJobDataMap().get("groupId");
        try {
            NotificationGroup activeGroup = notificationGroupRepository.findByIdAndStatus(groupId, BaseConstant.STATUS_ACTIVE).orElse(null);
            if (activeGroup == null) {
                log.debug("Notification group [{}] not found or not active, skip VictoriaLogs error check", groupId);
                return;
            }
            if (BaseConstant.NOTIFICATION_GROUP_CHECK_TYPE_COMPARISON.equals(activeGroup.getCheckType())) {
                log.info("Start checkComparisonAndAlert for group [{} - {}]", groupId, activeGroup.getName());
                checkComparisonAndAlert(activeGroup);
                log.info("Finished checkComparisonAndAlert for group [{} - {}]", groupId, activeGroup.getName());
            } else {
                log.info("Start checkErrorRateAndAlert for group [{} - {}]", groupId, activeGroup.getName());
                checkErrorRateAndAlert(activeGroup);
                log.info("Finished checkErrorRateAndAlert for group [{} - {}]", groupId, activeGroup.getName());
            }
        } catch (Exception e) {
            log.error("Error occurred in checkErrorRateAndAlert job", e);
        }
    }

    private void checkErrorRateAndAlert(NotificationGroup activeGroup) {
        if (activeGroup.getNotificationChannel() == null) {
            log.warn("Active notification group [{}] has no notification channel configured, skip VictoriaLogs error check", activeGroup.getName());
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

        String query = buildQuery(activeGroup.getTimeFrame(), activeGroup.getFilterQuery(), enabledTemplateIdsByApp.keySet(), queryTemplateById.values());
        Map<String, List<String>> breachLinesByApp = queryBreachesByApp(query, queryTemplateById,
                enabledTemplateIdsByApp, appNameByVictoriaAppId);
        if (breachLinesByApp.isEmpty()) {
            log.debug("No app crossed any notification query threshold in the last {}m", activeGroup.getTimeFrame());
            return;
        }

        List<String> apps = new ArrayList<>(breachLinesByApp.keySet());
        Collections.sort(apps);

        String title = "🚨 Cảnh báo hệ thống";
        int budget = messageBudget(activeGroup.getNotificationChannel().getType()) - title.length() - 1;
        if (budget <= 0) {
            log.error("Message limit for channel type [{}] is too small for title [{}], skip creating notification",
                    activeGroup.getNotificationChannel().getType(), title);
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
    }

    void checkComparisonAndAlert(NotificationGroup activeGroup) {
        List<NotificationRule> rules = notificationRuleRepository.findAllByNotificationGroupIdAndStatus(activeGroup.getId(), BaseConstant.STATUS_ACTIVE);
        if (rules.isEmpty()) {
            log.debug("Active notification group [{}] has no active notification rule, skip VictoriaLogs comparison check", activeGroup.getName());
            return;
        }

        Map<NotificationRule, List<NotificationRuleItem>> itemsByRule = new LinkedHashMap<>();
        Map<Long, QueryTemplate> queryTemplateById = new LinkedHashMap<>();
        Map<String, Set<Long>> enabledTemplateIdsByApp = new LinkedHashMap<>();
        for (NotificationRule rule : rules) {
            List<NotificationRuleItem> items = notificationRuleItemRepository.findAllByNotificationRuleIdOrderByOrdering(rule.getId());
            if (items == null || items.size() < 2) {
                log.warn("Notification rule [{}] has fewer than 2 items, skip comparison check", rule.getName());
                continue;
            }
            itemsByRule.put(rule, items);
            for (NotificationRuleItem item : items) {
                Applications application = item.getApplication();
                QueryTemplate queryTemplate = item.getQueryTemplate();
                queryTemplateById.putIfAbsent(queryTemplate.getId(), queryTemplate);
                enabledTemplateIdsByApp.computeIfAbsent(application.getVictoriaAppId(), k -> new LinkedHashSet<>())
                        .add(queryTemplate.getId());
            }
        }
        if (itemsByRule.isEmpty()) {
            log.debug("Active notification group [{}] has no usable notification rule, skip VictoriaLogs comparison check", activeGroup.getName());
            return;
        }

        String query = buildQuery(activeGroup.getTimeFrame(), activeGroup.getFilterQuery(), enabledTemplateIdsByApp.keySet(), queryTemplateById.values());
        log.info("Querying VictoriaLogs for comparison with query [{}]", query);
        List<VictoriaLogsStatsDto> rows = feignVictoriaLogsService.query(
                FeignConst.LOGIN_TYPE_NO_AUTH, VictoriaLogsQueryForm.of(query));

        Map<String, Integer> countByKey = new HashMap<>();
        if (rows != null) {
            for (VictoriaLogsStatsDto row : rows) {
                Set<Long> templateIds = enabledTemplateIdsByApp.get(row.getApplication());
                if (templateIds == null) {
                    continue;
                }
                for (Long templateId : templateIds) {
                    countByKey.put(row.getApplication() + "|" + templateId, row.count(String.valueOf(templateId)));
                }
            }
        }

        List<Notification> notifications = new ArrayList<>();
        for (Map.Entry<NotificationRule, List<NotificationRuleItem>> entry : itemsByRule.entrySet()) {
            NotificationRule rule = entry.getKey();
            List<NotificationRuleItem> items = entry.getValue();
            boolean violated = false;
            List<String> lines = new ArrayList<>();
            for (int i = 1; i < items.size(); i++) {
                NotificationRuleItem refItem = items.get(i - 1);
                NotificationRuleItem curItem = items.get(i);
                int refCount = countByKey.getOrDefault(itemKey(refItem), 0);
                int curCount = countByKey.getOrDefault(itemKey(curItem), 0);
                boolean pass = matches(curItem.getOperator(), refCount, curCount);
                if (!pass) {
                    violated = true;
                }
                lines.add(String.format("  • `%s` -> `%s`: %d vs %d [%s]",
                        refItem.getQueryTemplate().getName(), curItem.getQueryTemplate().getName(),
                        refCount, curCount, pass ? "OK" : "FAIL"));
            }
            if (violated) {
                Notification notification = new Notification();
                notification.setMessage(String.format("🚨 Cảnh báo so sánh: %s\n%s", rule.getName(), String.join("\n", lines)));
                notification.setState(BaseConstant.NOTIFICATION_STATE_SENT);
                notification.setNotificationGroup(activeGroup);
                notifications.add(notification);
            }
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
            log.info("Successfully created {} comparison notifications", notifications.size());
        }
    }

    private String itemKey(NotificationRuleItem item) {
        return item.getApplication().getVictoriaAppId() + "|" + item.getQueryTemplate().getId();
    }

    boolean matches(Integer operator, int ref, int cur) {
        if (BaseConstant.RULE_ITEM_OPERATOR_EQ.equals(operator)) {
            return cur == ref;
        } else if (BaseConstant.RULE_ITEM_OPERATOR_NEQ.equals(operator)) {
            return cur != ref;
        } else if (BaseConstant.RULE_ITEM_OPERATOR_GT.equals(operator)) {
            return cur > ref;
        } else if (BaseConstant.RULE_ITEM_OPERATOR_GTE.equals(operator)) {
            return cur >= ref;
        } else if (BaseConstant.RULE_ITEM_OPERATOR_LT.equals(operator)) {
            return cur < ref;
        } else if (BaseConstant.RULE_ITEM_OPERATOR_LTE.equals(operator)) {
            return cur <= ref;
        }
        log.warn("Unrecognized rule item operator [{}], treating comparison as a violation", operator);
        return false;
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
    String buildQuery(Integer timeFrameMinutes, String filterQuery, Collection<String> victoriaAppIds, Collection<QueryTemplate> queryTemplates) {
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

        StringBuilder prefix = new StringBuilder(String.format("_time:%dm", timeFrameMinutes));
        if (filterQuery != null && !filterQuery.trim().isEmpty()) {
            prefix.append(" ").append(filterQuery.trim());
        }
        prefix.append(" ").append(String.format("{%s:in(%s)}", BaseConstant.VICTORIALOGS_QUERY_APP_FIELD, apps));

        return String.format("%s | stats by (%s) %s",
                prefix, BaseConstant.VICTORIALOGS_QUERY_APP_FIELD, stats);
    }
}
