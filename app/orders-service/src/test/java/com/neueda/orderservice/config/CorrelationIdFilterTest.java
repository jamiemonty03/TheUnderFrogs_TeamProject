package com.neueda.orderservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    @DisplayName("A request without the header gets a new id in the MDC and the response header")
    void generatesIdWhenMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> idSeenByChain = new AtomicReference<>();

        filter.doFilter(new MockHttpServletRequest(), response, captureMdc(idSeenByChain));

        assertThat(idSeenByChain.get()).matches("[0-9a-f-]{36}");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo(idSeenByChain.get());
    }

    @Test
    @DisplayName("A valid caller-supplied id is reused")
    void reusesValidIncomingId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "client-req_123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> idSeenByChain = new AtomicReference<>();

        filter.doFilter(request, response, captureMdc(idSeenByChain));

        assertThat(idSeenByChain.get()).isEqualTo("client-req_123");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("client-req_123");
    }

    @ParameterizedTest
    @ValueSource(strings = {"has spaces", "line\nbreak", "{\"json\":1}", ""})
    @DisplayName("An unsafe or empty incoming id is replaced, so it can't be injected into the logs")
    void replacesUnsafeIncomingId(String unsafe) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, unsafe);
        AtomicReference<String> idSeenByChain = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), captureMdc(idSeenByChain));

        assertThat(idSeenByChain.get()).isNotEqualTo(unsafe).matches("[0-9a-f-]{36}");
    }

    @Test
    @DisplayName("An over-long incoming id is replaced")
    void replacesOverLongIncomingId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "a".repeat(65));
        AtomicReference<String> idSeenByChain = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), captureMdc(idSeenByChain));

        assertThat(idSeenByChain.get()).matches("[0-9a-f-]{36}");
    }

    @Test
    @DisplayName("The id is removed from the MDC after the request, even when the request fails")
    void clearsMdcAfterRequest() {
        FilterChain failingChain = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        try {
            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), failingChain);
        } catch (Exception expected) {
        }

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    private static FilterChain captureMdc(AtomicReference<String> target) {
        return (req, res) -> target.set(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
