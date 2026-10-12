package com.boutique.common.filter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;

class CorrelationIdFilterTests {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesUuidAddsItToMdcAndReturnsItInResponseHeader() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertDoesNotThrow(() -> filter.doFilter(new MockHttpServletRequest(), response, (request, res) -> {
            String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
            assertNotNull(correlationId);
            assertTrue(isUuid(correlationId));
            assertEquals(correlationId, ((MockHttpServletResponse) res)
                    .getHeader(CorrelationIdFilter.HEADER_NAME));
        }));

        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void clearsMdcWhenFilterChainThrows() {
        assertThrows(ServletException.class, () -> filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                (request, response) -> {
                    assertNotNull(MDC.get(CorrelationIdFilter.MDC_KEY));
                    throw new ServletException("downstream failure");
                }));

        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
