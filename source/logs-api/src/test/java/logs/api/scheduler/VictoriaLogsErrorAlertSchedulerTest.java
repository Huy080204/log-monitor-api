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
    void shouldBuildUnscopedLogsQlWhenApplicationIsNull() {
        NotificationQuery notificationQuery = notificationQuery(1L, "\"timeout\"", 5, null);

        String query = scheduler.buildQuery(Collections.singletonList(notificationQuery));

        assertThat(query).isEqualTo("_time:5m | stats by (application) count() if (_msg:\"timeout\") as \"1\"");
    }

    @Test
    void shouldEmbedApplicationConditionWhenApplicationIsPresent() {
        NotificationQuery notificationQuery = notificationQuery(1L, "\"timeout\"", 5, "AppOne");

        String query = scheduler.buildQuery(Collections.singletonList(notificationQuery));

        assertThat(query).isEqualTo("_time:5m | stats by (application) "
                + "count() if (_msg:\"timeout\" AND application:\"AppOne\") as \"1\"");
    }

    @Test
    void shouldNotDoubleCountAcrossApplicationsWhenMultipleQueriesScoped() {
        NotificationQuery first = notificationQuery(1L, "\"error\"", 3, "AppOne");
        NotificationQuery second = notificationQuery(2L, "\"error\"", 3, "AppTwo");

        String query = scheduler.buildQuery(Arrays.asList(first, second));

        assertThat(query).isEqualTo("_time:5m | stats by (application) "
                + "count() if (_msg:\"error\" AND application:\"AppOne\") as \"1\", "
                + "count() if (_msg:\"error\" AND application:\"AppTwo\") as \"2\"");
        assertThat(query).doesNotContain("AppOne\") as \"2\"");
        assertThat(query).doesNotContain("AppTwo\") as \"1\"");
    }

    private NotificationQuery notificationQuery(Long id, String queryPhrase, int count, String applicationName) {
        QueryTemplate queryTemplate = new QueryTemplate();
        queryTemplate.setName("query-" + id);
        queryTemplate.setQuery(queryPhrase);
        queryTemplate.setCount(count);
        if (applicationName != null) {
            Applications application = new Applications();
            application.setName(applicationName);
            queryTemplate.setApplication(application);
        }

        NotificationQuery notificationQuery = new NotificationQuery();
        notificationQuery.setId(id);
        notificationQuery.setQueryTemplate(queryTemplate);
        return notificationQuery;
    }
}
