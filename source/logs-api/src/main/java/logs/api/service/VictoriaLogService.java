package logs.api.service;

import logs.api.constant.BaseConstant;
import logs.api.dto.victorialogs.VictoriaLogsQueryForm;
import logs.api.dto.victorialogs.VictoriaLogsStatsDto;
import logs.api.model.QueryTemplate;
import logs.api.service.feign.FeignConst;
import logs.api.service.feign.FeignVictoriaLogsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class VictoriaLogService {

    @Autowired
    private FeignVictoriaLogsService feignVictoriaLogsService;

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

    // Run a LogsQL statement against VictoriaLogs
    public List<VictoriaLogsStatsDto> query(String logsQl) {
        return feignVictoriaLogsService.query(FeignConst.LOGIN_TYPE_NO_AUTH, VictoriaLogsQueryForm.of(logsQl));
    }
}
