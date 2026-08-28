package log.monitor.api.exception.oauth;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class CustomOauthExceptionSerializer extends StdSerializer<CustomOauthException> {

    public CustomOauthExceptionSerializer() {
        super(CustomOauthException.class);
    }
    @Override
    public void serialize(CustomOauthException value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("code", value.getHttpErrorCode()); // Mã lỗi HTTP
        jsonGenerator.writeStringField("message", value.getMessage()); // Thông điệp lỗi
        jsonGenerator.writeStringField("errorCode", value.getCode()); // Thêm mã lỗi tùy chỉnh
        jsonGenerator.writeObjectField("data", Arrays.asList(value.getOAuth2ErrorCode(), value.getMessage()));
        if (value.getAdditionalInformation() != null) {
            for (Map.Entry<String, String> entry : value.getAdditionalInformation().entrySet()) {
                jsonGenerator.writeStringField(entry.getKey(), entry.getValue());
            }
        }
        jsonGenerator.writeEndObject();
    }
}
