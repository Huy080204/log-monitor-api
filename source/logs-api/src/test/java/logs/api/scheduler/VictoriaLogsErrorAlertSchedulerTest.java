package logs.api.scheduler;

import logs.api.model.Applications;
import logs.api.model.NotificationQuery;
import logs.api.model.QueryTemplate;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class VictoriaLogsErrorAlertSchedulerTest {

    private final VictoriaLogsErrorAlertScheduler scheduler = new VictoriaLogsErrorAlertScheduler();

    @Test
    void shouldBuildRawLogsQlConditionScopedToNotificationQueryApplication() {
        NotificationQuery notificationQuery = notificationQuery(1L, "\"timeout\"", 5, "app-1", null);

        String query = scheduler.buildQuery(Collections.singletonList(notificationQuery));

        assertThat(query).isEqualTo("_time:5m | stats by (application) count() if (\"timeout\" AND application:\"app-1\") as \"1\"");
        assertThat(query).doesNotContain("_msg:");
    }

    @Test
    void shouldSourceApplicationFromNotificationQueryNotQueryTemplate() {
        NotificationQuery notificationQuery = notificationQuery(1L, "\"timeout\"", 5, "app-1", "template-app");

        String query = scheduler.buildQuery(Collections.singletonList(notificationQuery));

        assertThat(query).contains("application:\"app-1\"");
        assertThat(query).doesNotContain("template-app");
    }

    @Test
    void shouldNotDoubleCountAcrossApplicationsWhenMultipleQueriesScoped() {
        NotificationQuery first = notificationQuery(1L, "\"error\"", 3, "app-1", null);
        NotificationQuery second = notificationQuery(2L, "\"error\"", 3, "app-2", null);

        String query = scheduler.buildQuery(Arrays.asList(first, second));

        assertThat(query).isEqualTo("_time:5m | stats by (application) "
                + "count() if (\"error\" AND application:\"app-1\") as \"1\", "
                + "count() if (\"error\" AND application:\"app-2\") as \"2\"");
        assertThat(query).doesNotContain("app-1\") as \"2\"");
        assertThat(query).doesNotContain("app-2\") as \"1\"");
    }

    @Test
    void shouldSkipBlankQueryTemplateQuery() {
        NotificationQuery blank = notificationQuery(1L, "   ", 5, "app-1", null);
        NotificationQuery valid = notificationQuery(2L, "\"error\"", 3, "app-2", null);

        String query = scheduler.buildQuery(Arrays.asList(blank, valid));

        assertThat(query).isEqualTo("_time:5m | stats by (application) count() if (\"error\" AND application:\"app-2\") as \"2\"");
    }

    @Test
    void shouldSkipNotificationQueryWithNoApplication() {
        NotificationQuery noApplication = notificationQuery(1L, "\"error\"", 5, "app-1", null);
        noApplication.setApplication(null);
        NotificationQuery valid = notificationQuery(2L, "\"error\"", 3, "app-2", null);

        String query = scheduler.buildQuery(Arrays.asList(noApplication, valid));

        assertThat(query).isEqualTo("_time:5m | stats by (application) count() if (\"error\" AND application:\"app-2\") as \"2\"");
    }

    private NotificationQuery notificationQuery(Long id, String queryText, int count, String victoriaAppId, String queryTemplateVictoriaAppId) {
        QueryTemplate queryTemplate = new QueryTemplate();
        queryTemplate.setName("query-" + id);
        queryTemplate.setQuery(queryText);
        queryTemplate.setCount(count);
        if (queryTemplateVictoriaAppId != null) {
            Applications queryTemplateApplication = new Applications();
            queryTemplateApplication.setVictoriaAppId(queryTemplateVictoriaAppId);
            queryTemplate.setApplication(queryTemplateApplication);
        }

        Applications application = new Applications();
        application.setVictoriaAppId(victoriaAppId);

        NotificationQuery notificationQuery = new NotificationQuery();
        notificationQuery.setId(id);
        notificationQuery.setQueryTemplate(queryTemplate);
        notificationQuery.setApplication(application);
        return notificationQuery;
    }
}
