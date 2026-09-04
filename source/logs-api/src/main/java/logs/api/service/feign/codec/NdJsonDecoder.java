package logs.api.service.feign.codec;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NdJsonDecoder implements Decoder {

    private final ObjectMapper objectMapper;

    public NdJsonDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException {
        if (response.body() == null) {
            return Collections.emptyList();
        }
        if (!(type instanceof ParameterizedType)) {
            throw new DecodeException(response.status(),
                    "NdJsonDecoder chỉ hỗ trợ kiểu trả về List<T>, nhận được: " + type, response.request());
        }
        Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
        JavaType javaType = objectMapper.getTypeFactory().constructType(elementType);

        List<Object> results = new ArrayList<>();
        try (Reader reader = response.body().asReader(StandardCharsets.UTF_8)) {
            MappingIterator<Object> iterator = objectMapper.readerFor(javaType).readValues(reader);
            while (iterator.hasNext()) {
                results.add(iterator.next());
            }
        }
        return results;
    }
}
