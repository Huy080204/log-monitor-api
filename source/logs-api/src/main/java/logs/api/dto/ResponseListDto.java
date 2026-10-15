package logs.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ResponseListDto<T> {
    private static final long serialVersionUID = 1L;

    private T content;
    private Long totalElements;
    private Integer totalPages;


}
