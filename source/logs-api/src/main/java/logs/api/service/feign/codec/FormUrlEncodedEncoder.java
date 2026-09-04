package logs.api.service.feign.codec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class FormUrlEncodedEncoder implements Encoder {

    private final ObjectMapper objectMapper;

    public FormUrlEncodedEncoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void encode(Object object, Type bodyType, RequestTemplate template) {
        if (object == null) {
            template.body(new byte[0], StandardCharsets.UTF_8);
            return;
        }
        Map<String, Object> fields = objectMapper.convertValue(
                object, new TypeReference<Map<String, Object>>() {});

        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (body.length() > 0) {
                body.append('&');
            }
            body.append(urlEncode(entry.getKey()))
                .append('=')
                .append(urlEncode(String.valueOf(entry.getValue())));
        }
        template.body(body.toString().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new EncodeException("Cannot url-encode form field", e);
        }
    }
}
