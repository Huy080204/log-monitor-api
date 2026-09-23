package logs.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import logs.api.constant.BaseConstant;
import logs.api.dto.victorialogs.VictoriaLogsQueryForm;
import logs.api.dto.victorialogs.VictoriaLogsStatsDto;
import logs.api.model.NotificationRuleItem;
import logs.api.model.QueryTemplate;
import logs.api.service.feign.FeignConst;
import logs.api.service.feign.FeignVictoriaLogsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class VictoriaLogService {

    @Autowired
    private FeignVictoriaLogsService feignVictoriaLogsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${victorialogs.vm.ui.url}")
    private String victoriaLogsVmUiUrl;

    // Build LogsQL: time window + application:in(...) + one count() if per distinct QueryTemplate
    public String buildQuery(Integer timeFrameMinutes, String filterQuery, Set<String> victoriaAppIds, Collection<QueryTemplate> queryTemplates) {
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
        prefix.append(" ").append(String.format("{%s in (%s)}", BaseConstant.VICTORIALOGS_QUERY_APP_FIELD, apps));

        return String.format("%s | stats by (%s) %s",
                prefix, BaseConstant.VICTORIALOGS_QUERY_APP_FIELD, stats);
    }

    // Build a per-item comparison query: one segment per item, chained with flat `| union (...)`
    public String buildComparisonQuery(Integer timeFrameMinutes, String filterQuery, List<NotificationRuleItem> items) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            String segment = buildComparisonSegment(timeFrameMinutes, filterQuery, items.get(i));
            if (i == 0) {
                result.append(segment);
            } else {
                result.append(" | union (\n").append(segment).append("\n)");
            }
        }
        return result.toString();
    }

    private String buildComparisonSegment(Integer timeFrameMinutes, String filterQuery, NotificationRuleItem item) {
        StringBuilder segment = new StringBuilder(String.format("_time:%dm", timeFrameMinutes));
        if (filterQuery != null && !filterQuery.trim().isEmpty()) {
            segment.append(" ").append(filterQuery.trim());
        }
        segment.append(" ").append(String.format("{application=\"%s\"}", item.getApplication().getVictoriaAppId()));
        segment.append(" ").append(item.getQueryTemplate().getQuery().trim());
        segment.append(" | format \"").append(item.getId()).append("\" as item_id");
        return segment.toString();
    }

    // Run a LogsQL statement against VictoriaLogs
    public List<VictoriaLogsStatsDto> query(String logsQl) {
        return feignVictoriaLogsService.query(FeignConst.LOGIN_TYPE_NO_AUTH, VictoriaLogsQueryForm.of(logsQl));
    }

    // Build the LogsQL that reproduces one breach line's exact match: group filter + the query template's own condition.
    // App scoping and time range are NOT embedded here — vmui takes those as separate URL params (see buildExploreLink).
    public String buildExploreQuery(String filterQuery, QueryTemplate queryTemplate) {
        StringBuilder query = new StringBuilder();
        if (filterQuery != null && !filterQuery.trim().isEmpty()) {
            query.append(filterQuery.trim()).append(" ");
        }
        query.append(queryTemplate.getQuery().trim());
        return query.toString();
    }

    // Build a vmui link: domain + path + encoded query, plus an app stream filter (if any) and the relative time range
    public String buildExploreLink(String logsQl, String victoriaAppId, Integer timeFrameMinutes) {
        StringBuilder link = new StringBuilder(victoriaLogsVmUiUrl)
                .append(BaseConstant.VICTORIALOGS_VMUI_QUERY_PATH)
                .append(URLEncoder.encode(logsQl, StandardCharsets.UTF_8));
        String streamFilter = buildStreamFilterJson(victoriaAppId);
        if (streamFilter != null) {
            link.append("&extra_stream_filters=").append(URLEncoder.encode(streamFilter, StandardCharsets.UTF_8));
        }
        link.append("&g0.range_input=").append(URLEncoder.encode(timeFrameMinutes + "m", StandardCharsets.UTF_8));
        link.append("&g0.end_input=").append(URLEncoder.encode(Instant.now().truncatedTo(ChronoUnit.MILLIS).toString(), StandardCharsets.UTF_8));
        return link.toString();
    }

    // Build vmui's own stream-filter JSON shape: {"f":"application","o":"eq","v":"<victoriaAppId>"}
    private String buildStreamFilterJson(String victoriaAppId) {
        if (victoriaAppId == null || victoriaAppId.trim().isEmpty()) {
            return null;
        }
        Map<String, String> filter = new LinkedHashMap<>();
        filter.put("f", BaseConstant.VICTORIALOGS_QUERY_APP_FIELD);
        filter.put("o", "eq");
        filter.put("v", victoriaAppId);
        try {
            return objectMapper.writeValueAsString(filter);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize VictoriaLogs stream filter for app [{}]", victoriaAppId, e);
            return null;
        }
    }
}
