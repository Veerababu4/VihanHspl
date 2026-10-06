package com.vihan.hospital.common.logging;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Tags every log line of a request with a short request id (shown by the logging pattern and
 * returned in the {@code X-Request-Id} header), and writes one access-log line per API call.
 * Only the path is logged, never query strings or bodies, so mobile numbers and Aadhaar
 * can't leak into logs (DESIGN.md §12.4).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Request-Id";
	private static final String MDC_KEY = "requestId";
	private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9-]{1,36}");
	private static final Logger log = LoggerFactory.getLogger("access");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String incoming = request.getHeader(HEADER);
		String requestId = incoming != null && SAFE_ID.matcher(incoming).matches()
				? incoming
				: UUID.randomUUID().toString().substring(0, 8);
		long start = System.nanoTime();
		MDC.put(MDC_KEY, requestId);
		response.setHeader(HEADER, requestId);
		try {
			chain.doFilter(request, response);
		}
		finally {
			if (request.getRequestURI().startsWith("/api/")) {
				log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(), response.getStatus(),
						(System.nanoTime() - start) / 1_000_000);
			}
			MDC.remove(MDC_KEY);
		}
	}

}
