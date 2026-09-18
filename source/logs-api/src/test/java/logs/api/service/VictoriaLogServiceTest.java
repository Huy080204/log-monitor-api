package logs.api.service;

import logs.api.model.Applications;
import logs.api.model.NotificationRuleItem;
import logs.api.model.QueryTemplate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VictoriaLogServiceTest {

    private static final String SHARED_QUERY = "_msg:deliveryLogs* | extract \"SyncId: <sync_id>\" "
            + "| filter sync_id:!\"\" | stats count_uniq(sync_id) as value";

    private final VictoriaLogService victoriaLogService = new VictoriaLogService();

    private NotificationRuleItem buildItem(Long id, String victoriaAppId, String query) {
        Applications application = new Applications();
        application.setVictoriaAppId(victoriaAppId);

        QueryTemplate queryTemplate = new QueryTemplate();
        queryTemplate.setQuery(query);

        NotificationRuleItem item = new NotificationRuleItem();
        item.setId(id);
        item.setApplication(application);
        item.setQueryTemplate(queryTemplate);
        return item;
    }

    private String expectedSegment(Integer timeFrameMinutes, String filterQuery, String victoriaAppId, String query, Long id) {
        String filterToken = (filterQuery == null || filterQuery.trim().isEmpty())
                ? ""
                : filterQuery.trim() + " ";
        return "_time:" + timeFrameMinutes + "m " + filterToken
                + "{application=\"" + victoriaAppId + "\"} " + query.trim()
                + " | format \"" + id + "\" as item_id";
    }

    @Test
    void shouldChainTwoItemsWithSingleUnionWhenBuildingComparisonQuery() {
        NotificationRuleItem item11 = buildItem(11L, "sunlog-master", SHARED_QUERY);
        NotificationRuleItem item12 = buildItem(12L, "sunlog-slave", SHARED_QUERY);
        List<NotificationRuleItem> items = Arrays.asList(item11, item12);

        String result = victoriaLogService.buildComparisonQuery(360, "environment:prod", items);

        String segment1 = expectedSegment(360, "environment:prod", "sunlog-master", SHARED_QUERY, 11L);
        String segment2 = expectedSegment(360, "environment:prod", "sunlog-slave", SHARED_QUERY, 12L);

        assertThat(result).contains(segment1);
        assertThat(result).contains(segment2);
        assertThat(countOccurrences(result, "| union (")).isEqualTo(1);
        assertThat(result).isEqualTo(segment1 + " | union (\n" + segment2 + "\n)");
    }

    @Test
    void shouldChainThreeItemsFlatWithoutNestingWhenBuildingComparisonQuery() {
        NotificationRuleItem item11 = buildItem(11L, "sunlog-master", SHARED_QUERY);
        NotificationRuleItem item12 = buildItem(12L, "sunlog-slave", SHARED_QUERY);
        NotificationRuleItem item13 = buildItem(13L, "sunlog-worker", SHARED_QUERY);
        List<NotificationRuleItem> items = Arrays.asList(item11, item12, item13);

        String result = victoriaLogService.buildComparisonQuery(360, "environment:prod", items);

        String segment1 = expectedSegment(360, "environment:prod", "sunlog-master", SHARED_QUERY, 11L);
        String segment2 = expectedSegment(360, "environment:prod", "sunlog-slave", SHARED_QUERY, 12L);
        String segment3 = expectedSegment(360, "environment:prod", "sunlog-worker", SHARED_QUERY, 13L);

        String expected = segment1 + " | union (\n" + segment2 + "\n)" + " | union (\n" + segment3 + "\n)";

        assertThat(countOccurrences(result, "| union (")).isEqualTo(2);
        assertThat(result).isEqualTo(expected);
        assertThat(result).doesNotContain(segment2 + " | union (\n" + segment3);
    }

    @Test
    void shouldOmitFilterTokenAndAvoidLiteralNullWhenFilterQueryIsNull() {
        NotificationRuleItem item11 = buildItem(11L, "sunlog-master", SHARED_QUERY);
        List<NotificationRuleItem> items = new ArrayList<>();
        items.add(item11);

        String result = victoriaLogService.buildComparisonQuery(360, null, items);

        assertThat(result).contains("_time:360m {application=\"sunlog-master\"}");
        assertThat(result).doesNotContain("null");
    }

    private int countOccurrences(String haystack, String needle) {
        int count = 0;
        int index = 0;
        while ((index = haystack.indexOf(needle, index)) != -1) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
