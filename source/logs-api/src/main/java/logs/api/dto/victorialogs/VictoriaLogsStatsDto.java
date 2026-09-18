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

    @JsonProperty("item_id")
    private String itemId;

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
        return parse(counts.get(alias));
    }

    // Read the comparison row's first numeric field, whatever its alias
    public int firstCount() {
        if (counts.isEmpty()) {
            return 0;
        }
        return parse(counts.values().iterator().next());
    }

    private int parse(String value) {
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
