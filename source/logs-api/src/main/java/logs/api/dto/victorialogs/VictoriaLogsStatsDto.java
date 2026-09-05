package logs.api.dto.victorialogs;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class VictoriaLogsStatsDto {

    @JsonProperty("application")
    private String application;

    private final Map<String, String> counts = new LinkedHashMap<>();

    @JsonAnySetter
    public void putCount(String alias, String value) {
        counts.put(alias, value);
    }

    @JsonAnyGetter
    public Map<String, String> getCounts() {
        return counts;
    }

    public int count(String alias) {
        String value = counts.get(alias);
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
