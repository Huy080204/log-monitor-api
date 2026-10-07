package logs.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private static final String ORIGIN = "https://logs-fe.example.com";

    private final CorsFilter corsFilter = new CorsFilter(new SecurityConfig().corsConfigurationSource());

    @Test
    void shouldEchoRequestOriginWithCredentialsWhenCorsRequestArrives() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/account/list");
        request.addHeader(HttpHeaders.ORIGIN, ORIGIN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        corsFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(ORIGIN);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
    }

    @Test
    void shouldAllowPreflightWithClientTypeHeaderAndPatchMethod() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/v1/account/list");
        request.addHeader(HttpHeaders.ORIGIN, ORIGIN);
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization, content-type, x-client-type");
        MockHttpServletResponse response = new MockHttpServletResponse();

        corsFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(ORIGIN);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).contains("PATCH");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).containsIgnoringCase("x-client-type");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE)).isEqualTo("3600");
    }

    @Test
    void shouldRejectPreflightWhenRequestedHeaderIsNotAllowed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/v1/account/list");
        request.addHeader(HttpHeaders.ORIGIN, ORIGIN);
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "x-not-allowed");
        MockHttpServletResponse response = new MockHttpServletResponse();

        corsFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }
}
