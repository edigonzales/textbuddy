package app.textbuddy.config;

import app.textbuddy.web.error.TraceIdSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestTracingFilterTest {

    private final RequestTracingFilter filter = new RequestTracingFilter();

    @Test
    void keepsAValidCallerTraceIdAndClearsMdcAfterTheRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdSupport.TRACE_ID_HEADER, " team-request_42 ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(TraceIdSupport.TRACE_ID_HEADER)).isEqualTo("team-request_42");
        assertThat(request.getAttribute(TraceIdSupport.TRACE_ID_ATTRIBUTE)).isEqualTo("team-request_42");
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void replacesUnsafeOrOversizedCallerValues() throws Exception {
        assertReplaced("trace id with spaces");
        assertReplaced("x".repeat(65));
        assertReplaced("<script>");
    }

    private void assertReplaced(String callerValue) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdSupport.TRACE_ID_HEADER, callerValue);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(TraceIdSupport.TRACE_ID_HEADER))
                .isNotEqualTo(callerValue)
                .matches("[0-9a-f-]{36}");
    }
}
