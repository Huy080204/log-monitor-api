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

    @Test
    void shouldEchoRequestOriginWithCredentialsWhenCorsRequestArrives() throws Exception {
        CorsFilter corsFilter = new SecurityConfig().corsFilter().getFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/account/list");
        request.addHeader(HttpHeaders.ORIGIN, ORIGIN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        corsFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(ORIGIN);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
    }
}
