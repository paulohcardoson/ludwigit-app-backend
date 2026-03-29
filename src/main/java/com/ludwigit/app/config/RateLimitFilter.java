package com.ludwigit.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

	private static final String KEY_PREFIX = "rate_limit:";

	private final RedisTemplate<String, Object> redisTemplate;
	private final ObjectMapper objectMapper;
	private final int maxRequests;
	private final long windowSeconds;

	public RateLimitFilter(
		RedisTemplate<String, Object> redisTemplate,
		ObjectMapper objectMapper,
		@Value("${app.rate-limit.max-requests:60}") int maxRequests,
		@Value("${app.rate-limit.window-seconds:60}") long windowSeconds
	) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.maxRequests = maxRequests;
		this.windowSeconds = windowSeconds;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String ip = getClientIp(request);
		String key = KEY_PREFIX + ip;

		Long count = redisTemplate.opsForValue().increment(key);
		if (count == null) {
			filterChain.doFilter(request, response);
			return;
		}

		if (count == 1) {
			redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
		}

		if (count > maxRequests) {
			writeRateLimitResponse(response);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private String getClientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("message", "Too many requests. Please slow down and try again later.");
		body.put("code", HttpStatus.TOO_MANY_REQUESTS.value());
		body.put("timestamp", LocalDateTime.now().toString());

		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}
}
