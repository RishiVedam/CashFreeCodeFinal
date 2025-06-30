package com.example.payment.filter;

import com.newrelic.api.agent.NewRelic;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        // Get the trace ID from the New Relic agent.
        // (Depending on agent/version, use the appropriate API to extract the current trace identifier.)
        String traceId = NewRelic.getAgent().getTraceMetadata().getTraceId();
        String spanId = NewRelic.getAgent().getTraceMetadata().getSpanId();

        // Put trace ID in MDC so your SLF4J log pattern can include it.
        if (traceId != null) {
            MDC.put("traceId", traceId);
            MDC.put("spanId", spanId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.put("traceId", traceId);
            MDC.put("spanId", spanId);
        }
    }
}