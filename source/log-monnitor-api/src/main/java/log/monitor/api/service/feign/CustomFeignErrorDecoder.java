package log.monitor.api.service.feign;

import com.fasterxml.jackson.core.JsonProcessingException;
import log.monitor.api.exception.NotFoundException;
import feign.Request;
import feign.RetryableException;
import log.monitor.api.dto.ApiMessageDto;
import log.monitor.api.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import log.monitor.api.exception.UnauthorizedException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

@Component
@Slf4j
public class CustomFeignErrorDecoder implements ErrorDecoder {
    @Autowired
    private ObjectMapper objectMapper;
    @Override
    public Exception decode(String methodKey, Response response) {
        Request request = response.request();
        String message = "";
        try (InputStream inputStream = response.body().asInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            message = stringBuilder.toString();
        } catch (Exception e) {
            log.error("Error reading response body", e);
        }

        log.error("Feign error: " + message);
        switch (response.status()) {
            case 401:
                try {
                    ApiMessageDto<String> apiMessageDto = objectMapper.readValue(message, ApiMessageDto.class);
                    return new UnauthorizedException(apiMessageDto.getMessage(), apiMessageDto.getCode());
                } catch (JsonProcessingException e) {
                    log.error("Error parsing response body", e);
                    return new RuntimeException("Invalid response format: " + message);
                }
            case 403:
                return new RetryableException(403, message, request.httpMethod(), new Date(), request);
            case 400:
                try {
                    ApiMessageDto<String> apiMessageDto = objectMapper.readValue(message, ApiMessageDto.class);
                    return new BadRequestException(apiMessageDto.getMessage(), apiMessageDto.getCode());
                } catch (JsonProcessingException e) {
                    log.error("Error parsing response body", e);
                    return new RuntimeException("Invalid response format: " + message);
                }
            case 404:
                try {
                    ApiMessageDto<String> apiMessageDto = objectMapper.readValue(message, ApiMessageDto.class);
                    return new NotFoundException(apiMessageDto.getMessage(), apiMessageDto.getCode());
                } catch (JsonProcessingException e) {
                    log.error("Error parsing response body", e);
                    return new RuntimeException("Invalid response format: " + message);
                }
            default:
                return new RuntimeException(message);
        }
    }
}
