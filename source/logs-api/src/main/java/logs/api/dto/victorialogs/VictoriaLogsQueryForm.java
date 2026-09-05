package logs.api.dto.victorialogs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VictoriaLogsQueryForm {

    @JsonProperty("query")
    private String query;

    @JsonProperty("limit")
    private Integer limit;

    @JsonProperty("start")
    private String start;

    @JsonProperty("end")
    private String end;

    public static VictoriaLogsQueryForm of(String query) {
        VictoriaLogsQueryForm form = new VictoriaLogsQueryForm();
        form.setQuery(query);
        return form;
    }
}
