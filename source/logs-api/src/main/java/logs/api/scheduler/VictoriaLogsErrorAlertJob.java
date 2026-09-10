package logs.api.scheduler;

import logs.api.constant.BaseConstant;
import logs.api.dto.victorialogs.VictoriaLogsStatsDto;
import logs.api.model.*;
import logs.api.repository.*;
import logs.api.service.NotificationService;
import logs.api.service.VictoriaLogService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

@DisallowConcurrentExecution // Prevents concurrent execution of the same notification group's job
@Slf4j
public class VictoriaLogsErrorAlertJob implements Job {

    private static final String ALERT_TITLE = "🚨 Cảnh báo hệ thống";

    @Autowired
    private VictoriaLogService victoriaLogService;

    @Autowired
    private NotificationService notificationService;

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
            if (activeGroup.getNotificationChannel() == null) {
                log.warn("Active notification group [{}] has no notification channel configured, skip VictoriaLogs error check", activeGroup.getName());
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
            log.error("Error occurred in VictoriaLogs alert job for group [{}]", groupId, e);
        }
    }

    private void checkErrorRateAndAlert(NotificationGroup activeGroup) {
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

        Map<String, VictoriaLogsStatsDto> rowByApp = fetchRowsByApp(activeGroup,
                enabledTemplateIdsByApp.keySet(), queryTemplateById.values());
        Map<String, List<String>> breachLinesByApp = collectBreachLines(rowByApp, queryTemplateById,
                enabledTemplateIdsByApp, appNameByVictoriaAppId);
        if (breachLinesByApp.isEmpty()) {
            log.debug("No app crossed any notification query threshold in the last {}m", activeGroup.getTimeFrame());
            return;
        }

        List<String> apps = new ArrayList<>(breachLinesByApp.keySet());
        Collections.sort(apps);

        int budget = budgetFor(activeGroup);
        if (budget <= 0) {
            return;
        }

        List<Notification> notifications = buildNotifications(activeGroup, apps, breachLinesByApp, budget);
        notificationRepository.saveAll(notifications);
        log.info("Successfully created {} notifications for {} breaching app(s)", notifications.size(), apps.size());
    }

    private void checkComparisonAndAlert(NotificationGroup activeGroup) {
        List<NotificationRule> rules = notificationRuleRepository.findAllByNotificationGroupIdAndStatus(activeGroup.getId(), BaseConstant.STATUS_ACTIVE);
        if (rules.isEmpty()) {
            log.debug("Active notification group [{}] has no active notification rule, skip VictoriaLogs comparison check", activeGroup.getName());
            return;
        }

        Map<Long, List<NotificationRuleItem>> itemsByRuleId = loadItemsByRuleId(rules);

        List<NotificationRule> usableRules = new ArrayList<>(rules.size());
        for (NotificationRule rule : rules) {
            List<NotificationRuleItem> items = itemsByRuleId.get(rule.getId());
            if (items == null || items.size() < 2) {
                log.warn("Notification rule [{}] has fewer than 2 items, skip comparison check", rule.getName());
                continue;
            }
            usableRules.add(rule);
        }
        if (usableRules.isEmpty()) {
            log.debug("Active notification group [{}] has no usable notification rule, skip VictoriaLogs comparison check", activeGroup.getName());
            return;
        }

        Map<String, VictoriaLogsStatsDto> rowByApp = fetchRowsByApp(activeGroup,
                collectVictoriaAppIdsByRules(usableRules, itemsByRuleId), collectQueryTemplates(usableRules, itemsByRuleId));

        int budget = budgetFor(activeGroup);
        if (budget <= 0) {
            return;
        }

        List<Notification> notifications = new ArrayList<>();
        for (NotificationRule rule : usableRules) {
            List<String> lines = buildAlertLines(rule, itemsByRuleId.get(rule.getId()), rowByApp);
            if (lines.isEmpty()) {
                continue;
            }
            notifications.addAll(buildNotifications(activeGroup, Collections.singletonList(rule.getName()),
                    Collections.singletonMap(rule.getName(), lines), budget));
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
            log.info("Successfully created {} comparison notifications", notifications.size());
        }
    }

    private Map<Long, List<NotificationRuleItem>> loadItemsByRuleId(List<NotificationRule> rules) {
        List<Long> ruleIds = rules.stream().map(NotificationRule::getId).collect(Collectors.toList());
        List<NotificationRuleItem> items = notificationRuleItemRepository.findAllByNotificationRuleIdInFetchingRefs(ruleIds);
        return items.stream()
                .collect(
                        Collectors.groupingBy(
                                item -> item.getNotificationRule().getId())
                );
    }

    private Set<String> collectVictoriaAppIdsByRules(List<NotificationRule> usableRules,
                                                     Map<Long, List<NotificationRuleItem>> itemsByRuleId) {
        Set<String> victoriaAppIds = new LinkedHashSet<>();
        for (NotificationRule rule : usableRules) {
            for (NotificationRuleItem item : itemsByRuleId.get(rule.getId())) {
                victoriaAppIds.add(item.getApplication().getVictoriaAppId());
            }
        }
        return victoriaAppIds;
    }

    private Collection<QueryTemplate> collectQueryTemplates(List<NotificationRule> usableRules,
                                                            Map<Long, List<NotificationRuleItem>> itemsByRuleId) {
        Map<Long, QueryTemplate> queryTemplateById = new LinkedHashMap<>();
        for (NotificationRule rule : usableRules) {
            for (NotificationRuleItem item : itemsByRuleId.get(rule.getId())) {
                queryTemplateById.putIfAbsent(item.getQueryTemplate().getId(), item.getQueryTemplate());
            }
        }
        return queryTemplateById.values();
    }

    private Map<String, VictoriaLogsStatsDto> fetchRowsByApp(NotificationGroup activeGroup,
                                                             Set<String> victoriaAppIds,
                                                             Collection<QueryTemplate> queryTemplates) {
        String query = victoriaLogService.buildQuery(activeGroup.getTimeFrame(), activeGroup.getFilterQuery(),
                victoriaAppIds, queryTemplates);
        log.info("Querying VictoriaLogs for group [{}] with query [{}]", activeGroup.getName(), query);

        Map<String, VictoriaLogsStatsDto> rowByApp = new LinkedHashMap<>();
        List<VictoriaLogsStatsDto> rows = victoriaLogService.query(query);
        if (rows == null || rows.isEmpty()) {
            log.warn("VictoriaLogs returned no row for group [{}] in the last {}m", activeGroup.getName(), activeGroup.getTimeFrame());
            return rowByApp;
        }
        for (VictoriaLogsStatsDto row : rows) {
            rowByApp.put(row.getApplication(), row);
        }
        return rowByApp;
    }

    // Keep only (app, template) pairs enabled in enabledTemplateIdsByApp whose count hit the threshold
    private Map<String, List<String>> collectBreachLines(Map<String, VictoriaLogsStatsDto> rowByApp,
                                                         Map<Long, QueryTemplate> queryTemplateById,
                                                         Map<String, Set<Long>> enabledTemplateIdsByApp,
                                                         Map<String, String> appNameByVictoriaAppId) {
        Map<String, List<String>> breachLinesByApp = new LinkedHashMap<>();
        for (VictoriaLogsStatsDto row : rowByApp.values()) {
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

    private List<String> buildAlertLines(NotificationRule rule, List<NotificationRuleItem> items,
                                         Map<String, VictoriaLogsStatsDto> rowByApp) {
        int[] counts = resolveCounts(items, rowByApp);
        if (isAllZero(counts)) {
            log.debug("Notification rule [{}] has no log in the last window, skip comparison", rule.getName());
            return Collections.emptyList();
        }

        boolean shouldAlert = false;
        List<String> lines = new ArrayList<>(items.size() - 1);
        for (int i = 1; i < items.size(); i++) {
            NotificationRuleItem previous = items.get(i - 1);
            NotificationRuleItem current = items.get(i);
            Boolean matched = matches(current.getOperator(), counts[i - 1], counts[i]);
            if (matched == null) {
                log.error("Notification rule [{}] item [{}] has an unsupported operator [{}], skip this pair",
                        rule.getName(), current.getId(), current.getOperator());
                continue;
            }
            if (matched) {
                shouldAlert = true;
            }
            lines.add(String.format("  • `%s / %s`: %d %s `%s / %s`: %d",
                    previous.getApplication().getName(), previous.getQueryTemplate().getName(), counts[i - 1],
                    operatorSymbol(current.getOperator()),
                    current.getApplication().getName(), current.getQueryTemplate().getName(), counts[i]));
        }
        return shouldAlert ? lines : Collections.emptyList();
    }

    private boolean isAllZero(int[] counts) {
        for (int count : counts) {
            if (count != 0) {
                return false;
            }
        }
        return true;
    }

    // Resolve each item's count once so the pair loop above is pure array indexing
    private int[] resolveCounts(List<NotificationRuleItem> items, Map<String, VictoriaLogsStatsDto> rowByApp) {
        int[] counts = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            NotificationRuleItem item = items.get(i);
            VictoriaLogsStatsDto row = rowByApp.get(item.getApplication().getVictoriaAppId());
            // An application missing from the response counts as 0 rather than being skipped
            counts[i] = row == null ? 0 : row.count(String.valueOf(item.getQueryTemplate().getId()));
        }
        return counts;
    }

    private Boolean matches(Integer operator, int previousCount, int currentCount) {
        if (BaseConstant.RULE_ITEM_OPERATOR_EQ.equals(operator)) {
            return previousCount == currentCount;
        } else if (BaseConstant.RULE_ITEM_OPERATOR_NEQ.equals(operator)) {
            return previousCount != currentCount;
        } else if (BaseConstant.RULE_ITEM_OPERATOR_GT.equals(operator)) {
            return previousCount > currentCount;
        } else if (BaseConstant.RULE_ITEM_OPERATOR_GTE.equals(operator)) {
            return previousCount >= currentCount;
        } else if (BaseConstant.RULE_ITEM_OPERATOR_LT.equals(operator)) {
            return previousCount < currentCount;
        } else if (BaseConstant.RULE_ITEM_OPERATOR_LTE.equals(operator)) {
            return previousCount <= currentCount;
        }
        return null;
    }

    private String operatorSymbol(Integer operator) {
        if (BaseConstant.RULE_ITEM_OPERATOR_EQ.equals(operator)) {
            return "=";
        } else if (BaseConstant.RULE_ITEM_OPERATOR_NEQ.equals(operator)) {
            return "!=";
        } else if (BaseConstant.RULE_ITEM_OPERATOR_GT.equals(operator)) {
            return ">";
        } else if (BaseConstant.RULE_ITEM_OPERATOR_GTE.equals(operator)) {
            return ">=";
        } else if (BaseConstant.RULE_ITEM_OPERATOR_LT.equals(operator)) {
            return "<";
        } else if (BaseConstant.RULE_ITEM_OPERATOR_LTE.equals(operator)) {
            return "<=";
        }
        return "?";
    }

    private int budgetFor(NotificationGroup activeGroup) {
        int budget = notificationService.messageBudget(activeGroup.getNotificationChannel().getType())
                - ALERT_TITLE.length() - 1;
        if (budget <= 0) {
            log.error("Message limit for channel type [{}] is too small for title [{}], skip creating notification",
                    activeGroup.getNotificationChannel().getType(), ALERT_TITLE);
        }
        return budget;
    }

    private List<Notification> buildNotifications(NotificationGroup activeGroup, List<String> headers,
                                                  Map<String, List<String>> linesByHeader, int budget) {
        List<Notification> notifications = new ArrayList<>();
        for (String bodyPart : notificationService.packBodies(headers, linesByHeader, budget)) {
            Notification notification = new Notification();
            notification.setMessage(ALERT_TITLE + "\n" + bodyPart);
            notification.setState(BaseConstant.NOTIFICATION_STATE_SENT);
            notification.setNotificationGroup(activeGroup);
            notifications.add(notification);
        }
        return notifications;
    }
}
