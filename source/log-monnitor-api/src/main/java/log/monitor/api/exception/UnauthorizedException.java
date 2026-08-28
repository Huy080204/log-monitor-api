package log.monitor.api.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnauthorizedException extends RuntimeException {
    private String code;

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, String code) {
        super(message);
        this.code = code;
    }
}
