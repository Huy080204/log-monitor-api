package logs.api.scheduler;

import logs.api.dto.victorialogs.VictoriaLogsStatsDto;
import logs.api.model.Applications;
import logs.api.model.QueryTemplate;
import logs.api.service.feign.FeignVictoriaLogsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VictoriaLogsErrorAlertSchedulerTest {

    @Mock
    private FeignVictoriaLogsService feignVictoriaLogsService;

    @InjectMocks
    private VictoriaLogsErrorAlertScheduler scheduler;

    @Test
    void shouldBuildBatchedQueryWhenSingleApplicationAndTemplate() {
        Applications app1 = application(1L, "app-1");
        QueryTemplate template = queryTemplate(10L, "\"timeout\"", 5);

        String query = scheduler.buildQuery(Collections.singletonList(app1), Collections.singletonList(template));

        assertThat(query).isEqualTo("_time:5m application:in(\"app-1\") | stats by (application) count() if (\"timeout\") as \"10\"");
        assertThat(query).doesNotContain("_msg:");
    }

    @Test
    void shouldEmitTemplateConditionOnceWhenSharedAcrossApplications() {
        Applications app1 = application(1L, "app-1");
        Applications app2 = application(2L, "app-2");
        QueryTemplate template = queryTemplate(10L, "\"error\"", 3);

        String query = scheduler.buildQuery(Arrays.asList(app1, app2), Collections.singletonList(template));

        assertThat(query).contains("application:in(\"app-1\", \"app-2\")");
        assertThat(query).containsOnlyOnce("as \"10\"");
    }

    @Test
    void shouldListEachApplicationAndTemplateOnceWhenMultipleOfEach() {
        Applications app1 = application(1L, "app-1");
        Applications app2 = application(2L, "app-2");
        QueryTemplate template1 = queryTemplate(10L, "\"error\"", 3);
        QueryTemplate template2 = queryTemplate(20L, "\"timeout\"", 5);

        String query = scheduler.buildQuery(Arrays.asList(app1, app2), Arrays.asList(template1, template2));

        assertThat(query).isEqualTo("_time:5m application:in(\"app-1\", \"app-2\") | stats by (application) "
                + "count() if (\"error\") as \"10\", count() if (\"timeout\") as \"20\"");
    }

    @Test
    void shouldReportBreachWhenTemplateEnabledForApplication() {
        Applications app1 = application(1L, "app-1");
        QueryTemplate template = queryTemplate(10L, "\"error\"", 3);
        Map<Long, Set<Long>> enabledTemplateIdsByAppId = enabledMap(app1.getId(), template.getId());
        when(feignVictoriaLogsService.query(any(), any())).thenReturn(Collections.singletonList(row("app-1", "10", 5)));

        Map<String, List<String>> breaches = scheduler.queryBreachesByApp("query", Collections.singletonList(app1),
                Collections.singletonList(template), enabledTemplateIdsByAppId);

        assertThat(breaches).containsKey("app-1");
        assertThat(breaches.get("app-1")).hasSize(1);
        assertThat(breaches.get("app-1").get(0)).contains(template.getName()).contains("5");
    }

    @Test
    void shouldNotReportBreachWhenTemplateNotEnabledForApplication() {
        Applications app1 = application(1L, "app-1");
        QueryTemplate template = queryTemplate(10L, "\"error\"", 3);
        Map<Long, Set<Long>> enabledTemplateIdsByAppId = new HashMap<>();
        when(feignVictoriaLogsService.query(any(), any())).thenReturn(Collections.singletonList(row("app-1", "10", 5)));

        Map<String, List<String>> breaches = scheduler.queryBreachesByApp("query", Collections.singletonList(app1),
                Collections.singletonList(template), enabledTemplateIdsByAppId);

        assertThat(breaches).isEmpty();
    }

    @Test
    void shouldReportBreachForEachApplicationWhenTemplateSharedAcrossApplications() {
        Applications app1 = application(1L, "app-1");
        Applications app2 = application(2L, "app-2");
        QueryTemplate template = queryTemplate(10L, "\"error\"", 3);
        Map<Long, Set<Long>> enabledTemplateIdsByAppId = new HashMap<>();
        enabledTemplateIdsByAppId.put(app1.getId(), new HashSet<>(Collections.singletonList(template.getId())));
        enabledTemplateIdsByAppId.put(app2.getId(), new HashSet<>(Collections.singletonList(template.getId())));
        when(feignVictoriaLogsService.query(any(), any())).thenReturn(Arrays.asList(
                row("app-1", "10", 5), row("app-2", "10", 4)));

        Map<String, List<String>> breaches = scheduler.queryBreachesByApp("query", Arrays.asList(app1, app2),
                Collections.singletonList(template), enabledTemplateIdsByAppId);

        assertThat(breaches).containsKeys("app-1", "app-2");
    }

    private Map<Long, Set<Long>> enabledMap(Long applicationId, Long templateId) {
        Map<Long, Set<Long>> map = new HashMap<>();
        map.put(applicationId, new HashSet<>(Collections.singletonList(templateId)));
        return map;
    }

    private VictoriaLogsStatsDto row(String victoriaAppId, String alias, int count) {
        VictoriaLogsStatsDto row = new VictoriaLogsStatsDto();
        row.setApplication(victoriaAppId);
        row.putCount(alias, String.valueOf(count));
        return row;
    }

    private Applications application(Long id, String victoriaAppId) {
        Applications application = new Applications();
        application.setId(id);
        application.setVictoriaAppId(victoriaAppId);
        return application;
    }

    private QueryTemplate queryTemplate(Long id, String query, int count) {
        QueryTemplate queryTemplate = new QueryTemplate();
        queryTemplate.setId(id);
        queryTemplate.setName("query-" + id);
        queryTemplate.setQuery(query);
        queryTemplate.setCount(count);
        return queryTemplate;
    }
}
