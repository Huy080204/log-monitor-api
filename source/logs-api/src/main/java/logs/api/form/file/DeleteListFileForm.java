package logs.api.form.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteListFileForm {
    @NotNull(message = "files cannot be null")
    private List<String> files;
}
