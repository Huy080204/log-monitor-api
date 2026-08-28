package log.monitor.api.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import log.monitor.api.constant.BaseConstant;
import log.monitor.api.service.SlackAlertService;
import log.monitor.api.service.feign.FeignConst;
import log.monitor.api.service.feign.FeignVictoriaLogsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class VictoriaLogsErrorAlertScheduler {

    @Autowired
    private FeignVictoriaLogsService feignVictoriaLogsService;

    @Autowired
    private SlackAlertService slackAlertService;

    @Autowired
    private ObjectMapper objectMapper;

    @Scheduled(cron = "0 */5 * * * *")
    public void checkErrorRateAndAlert() {
        String query = buildQuery();
        Map<String, Integer> errorCountsByApp;
        try {
            errorCountsByApp = queryErrorCountsByApp(query);
        } catch (Exception e) {
            log.error("Failed to query VictoriaLogs [{}]", query, e);
            return;
        }

        List<Map.Entry<String, Integer>> breaching = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : errorCountsByApp.entrySet()) {
            if (entry.getValue() >= BaseConstant.VICTORIALOGS_ERROR_THRESHOLD) {
                breaching.add(entry);
            }
        }

        if (breaching.isEmpty()) {
            log.debug("No app crossed the error threshold ({}) in the last {}",
                    BaseConstant.VICTORIALOGS_ERROR_THRESHOLD, BaseConstant.VICTORIALOGS_QUERY_WINDOW);
            return;
        }

        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : breaching) {
            lines.add(String.format(":red_circle: *%s* — %d lỗi trong %s",
                    entry.getKey(), entry.getValue(), BaseConstant.VICTORIALOGS_QUERY_WINDOW));
        }
        Collections.sort(lines);

        String title = String.format("🚨 %d app vượt ngưỡng %d lỗi / %s",
                breaching.size(), BaseConstant.VICTORIALOGS_ERROR_THRESHOLD, BaseConstant.VICTORIALOGS_QUERY_WINDOW);
        slackAlertService.sendMessage(title, lines);
    }

    private Map<String, Integer> queryErrorCountsByApp(String query) throws Exception {
        String rawBody = feignVictoriaLogsService.query(FeignConst.LOGIN_TYPE_NO_AUTH, query);
        Map<String, Integer> counts = new HashMap<>();
        if (rawBody == null || rawBody.trim().isEmpty()) {
            return counts;
        }

        MappingIterator<Map<String, String>> lines = objectMapper
                .readerFor(new TypeReference<Map<String, String>>() {})
                .readValues(rawBody);
        while (lines.hasNext()) {
            Map<String, String> line = lines.next();
            String app = line.getOrDefault(BaseConstant.VICTORIALOGS_QUERY_APP_FIELD, "unknown");
            counts.merge(app, 1, Integer::sum);
        }
        return counts;
    }

    private String buildQuery() {
        return String.format("_time:%s %s:%s | fields %s",
                BaseConstant.VICTORIALOGS_QUERY_WINDOW, BaseConstant.VICTORIALOGS_ERROR_FIELD,
                BaseConstant.VICTORIALOGS_ERROR_VALUE, BaseConstant.VICTORIALOGS_QUERY_APP_FIELD);
    }
}
