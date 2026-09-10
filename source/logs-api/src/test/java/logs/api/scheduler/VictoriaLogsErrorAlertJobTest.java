package logs.api.scheduler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import logs.api.constant.BaseConstant;
import logs.api.dto.victorialogs.VictoriaLogsQueryForm;
import logs.api.dto.victorialogs.VictoriaLogsStatsDto;
import logs.api.model.Applications;
import logs.api.model.NotificationGroup;
import logs.api.model.NotificationRule;
import logs.api.model.NotificationRuleItem;
import logs.api.model.Notification;
import logs.api.model.QueryTemplate;
import logs.api.repository.NotificationGroupRepository;
import logs.api.repository.NotificationQueryRepository;
import logs.api.repository.NotificationRepository;
import logs.api.repository.NotificationRuleItemRepository;
import logs.api.repository.NotificationRuleRepository;
import logs.api.service.feign.FeignVictoriaLogsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VictoriaLogsErrorAlertJobTest {

    @Mock
    private FeignVictoriaLogsService feignVictoriaLogsService;

    @Mock
    private NotificationGroupRepository notificationGroupRepository;

    @Mock
    private NotificationQueryRepository notificationQueryRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationRuleRepository notificationRuleRepository;

    @Mock
    private NotificationRuleItemRepository notificationRuleItemRepository;

    @InjectMocks
    private VictoriaLogsErrorAlertJob job;

    @Test
    void matchesEqOperator() {
        assertThat(job.matches(BaseConstant.RULE_ITEM_OPERATOR_EQ, 5, 5)).isTrue();
        assertThat(job.matches(BaseConstant.RULE_ITEM_OPERATOR_EQ, 5, 6)).isFalse();
    }

    @Test
    void matchesNeqOperator() {
        assertThat(job.matches(BaseConstant.RULE_ITEM_OPERATOR_NEQ, 5, 6)).isTrue();
        assertThat(job.matches(BaseConstant.RULE_ITEM_OPERATOR_NEQ, 5, 5)).isFalse();
    }

    @Test
    void matchesGtOperator() {
        assertThat(job.matches(BaseConstant.RULE_ITEM_OPERATOR_GT, 5, 6)).isTrue();
        assertThat(job.matches(BaseConstant.RULE_ITEM_OPERATOR_GT, 6, 5)).isFalse();
    }

    @Test
    void matchesGteOperator() {
        assertThat(job.matches(BaseConstant.RULE_ITEM_OPERATOR_GTE, 5, 5)).isTrue();
        assertThat(job.matches(BaseConstant.RULE_ITEM_OPERATOR_GTE, 6, 5)).isFalse();
    }

    @Test
    void matchesLtOperator() {
        assertThat(job.matches(BaseConstant.RULE_ITEM_OPERATOR_LT, 6, 5)).isTrue();
        assertThat(job.matches(BaseConstant.RULE_ITEM_OPERATOR_LT, 5, 6)).isFalse();
    }

    @Test
    void matchesLteOperator() {
        assertThat(job.matches(BaseConstant.RULE_ITEM_OPERATOR_LTE, 5, 5)).isTrue();
        assertThat(job.matches(BaseConstant.RULE_ITEM_OPERATOR_LTE, 5, 6)).isFalse();
    }

    @Test
    void skipsRuleWithFewerThanTwoItems() {
        NotificationGroup group = new NotificationGroup();
        group.setId(1L);
        group.setName("group-1");
        group.setCheckType(BaseConstant.NOTIFICATION_GROUP_CHECK_TYPE_COMPARISON);
        group.setTimeFrame(15);

        NotificationRule rule = new NotificationRule();
        rule.setId(10L);
        rule.setName("rule-1");
        rule.setNotificationGroup(group);

        NotificationRuleItem onlyItem = new NotificationRuleItem();
        onlyItem.setId(100L);
        onlyItem.setOrdering(0);

        when(notificationRuleRepository.findAllByNotificationGroupIdAndStatus(group.getId(), BaseConstant.STATUS_ACTIVE))
                .thenReturn(Collections.singletonList(rule));
        when(notificationRuleItemRepository.findAllByNotificationRuleIdOrderByOrdering(rule.getId()))
                .thenReturn(Collections.singletonList(onlyItem));

        Logger logger = (Logger) LoggerFactory.getLogger(VictoriaLogsErrorAlertJob.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        job.checkComparisonAndAlert(group);

        verify(notificationRepository, never()).saveAll(any());
        assertThat(appender.list)
                .anyMatch(event -> event.getLevel() == Level.WARN
                        && event.getFormattedMessage().contains("rule-1"));

        logger.detachAppender(appender);
    }

    @Test
    void adjacentPairViolationProducesNotification() {
        NotificationGroup group = new NotificationGroup();
        group.setId(2L);
        group.setName("group-2");
        group.setCheckType(BaseConstant.NOTIFICATION_GROUP_CHECK_TYPE_COMPARISON);
        group.setTimeFrame(15);

        Applications application = new Applications();
        application.setId(20L);
        application.setVictoriaAppId("app-victoria-id");
        application.setName("app-name");

        QueryTemplate queryTemplate = new QueryTemplate();
        queryTemplate.setId(30L);
        queryTemplate.setName("template-1");
        queryTemplate.setQuery("level:error");

        NotificationRuleItem item0 = new NotificationRuleItem();
        item0.setId(200L);
        item0.setOrdering(0);
        item0.setApplication(application);
        item0.setQueryTemplate(queryTemplate);
        item0.setOperator(null);

        NotificationRuleItem item1 = new NotificationRuleItem();
        item1.setId(201L);
        item1.setOrdering(1);
        item1.setApplication(application);
        item1.setQueryTemplate(queryTemplate);
        item1.setOperator(BaseConstant.RULE_ITEM_OPERATOR_GT);

        NotificationRule rule = new NotificationRule();
        rule.setId(11L);
        rule.setName("rule-2");
        rule.setNotificationGroup(group);

        List<NotificationRuleItem> items = Arrays.asList(item0, item1);

        VictoriaLogsStatsDto statsDto = new VictoriaLogsStatsDto();
        statsDto.setApplication(application.getVictoriaAppId());
        statsDto.putCount("30", "10");

        when(notificationRuleRepository.findAllByNotificationGroupIdAndStatus(group.getId(), BaseConstant.STATUS_ACTIVE))
                .thenReturn(Collections.singletonList(rule));
        when(notificationRuleItemRepository.findAllByNotificationRuleIdOrderByOrdering(rule.getId()))
                .thenReturn(items);
        when(feignVictoriaLogsService.query(any(), any(VictoriaLogsQueryForm.class)))
                .thenReturn(Collections.singletonList(statsDto));

        job.checkComparisonAndAlert(group);

        verify(notificationRepository).saveAll(argThatSavesOneNotificationMentioning("rule-2"));
    }

    private static List<Notification> argThatSavesOneNotificationMentioning(String text) {
        return org.mockito.ArgumentMatchers.argThat(notifications ->
                notifications != null
                        && notifications.size() == 1
                        && notifications.get(0).getMessage().contains(text));
    }

    @Test
    void buildQueryEmitsBracedApplicationFilterRightAfterTimeAndFilterQuery() {
        QueryTemplate queryTemplate = new QueryTemplate();
        queryTemplate.setId(30L);
        queryTemplate.setName("template-1");
        queryTemplate.setQuery("level:error");
        queryTemplate.setCount(5);

        String query = job.buildQuery(15, "env:prod", Collections.singletonList("app-victoria-id"),
                Collections.singletonList(queryTemplate));

        String timePrefix = "_time:15m";
        String filterQuery = "env:prod";
        String bracedAppFilter = String.format("{%s:in(\"app-victoria-id\")}", BaseConstant.VICTORIALOGS_QUERY_APP_FIELD);

        assertThat(query).contains(timePrefix);
        assertThat(query).contains(bracedAppFilter);
        assertThat(query.indexOf(timePrefix)).isLessThan(query.indexOf(filterQuery));
        assertThat(query.indexOf(filterQuery)).isLessThan(query.indexOf(bracedAppFilter));
        // the old bare (unbraced) syntax must not appear anywhere the new braced form doesn't
        // already account for — i.e. no "application:in(...)" occurrence lacking its leading "{"
        int bareIndex = query.indexOf(String.format("%s:in(\"app-victoria-id\")",
                BaseConstant.VICTORIALOGS_QUERY_APP_FIELD));
        assertThat(bareIndex).isGreaterThan(0);
        assertThat(query.charAt(bareIndex - 1)).isEqualTo('{');
    }
}
