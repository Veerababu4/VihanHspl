package com.vihan.hospital.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.vihan.hospital.common.error.ApiError;

import tools.jackson.databind.ObjectMapper;

/**
 * M1 baseline: public endpoints are open, everything else is denied with a JSON 401.
 * M3 adds the JWT filter, /api/auth/** and role rules (DESIGN.md §12).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private static final String[] PUBLIC_PATHS = {
			"/api/public/**",
			"/files/**",
			"/actuator/health/**",
			"/actuator/info",
			"/v3/api-docs/**",      // springdoc; only enabled in the local profile
			"/swagger-ui/**",
			"/swagger-ui.html",
			"/error"
	};

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)          // stateless JSON API, no session cookies
			.cors(Customizer.withDefaults())
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(PUBLIC_PATHS).permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(e -> e
				.authenticationEntryPoint((req, res, ex) -> writeError(res, objectMapper, HttpStatus.UNAUTHORIZED,
						"UNAUTHORIZED", "Please log in"))
				.accessDeniedHandler((req, res, ex) -> writeError(res, objectMapper, HttpStatus.FORBIDDEN,
						"FORBIDDEN", "You do not have permission for this action")));
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(AppProperties props) {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(props.corsAllowedOrigins());
		cors.addAllowedMethod("*");
		cors.addAllowedHeader("*");
		cors.setAllowCredentials(true);       // refresh-token cookie (M3)
		cors.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", cors);
		return source;
	}

	private static void writeError(HttpServletResponse res, ObjectMapper mapper, HttpStatus status, String code,
			String message) throws IOException {
		res.setStatus(status.value());
		res.setContentType(MediaType.APPLICATION_JSON_VALUE);
		mapper.writeValue(res.getOutputStream(), ApiError.of(status.value(), code, message));
	}

}
