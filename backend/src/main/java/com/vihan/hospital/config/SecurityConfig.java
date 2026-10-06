package com.vihan.hospital.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.vihan.hospital.common.error.ApiError;
import com.vihan.hospital.security.JwtConfig;

import tools.jackson.databind.ObjectMapper;

/**
 * URL-level rules (DESIGN.md §12.3). Controllers add @PreAuthorize and services add ownership
 * checks ("doctor sees own data"), so access is checked in more than one place.
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

	/** Endpoints that work without an access token: they use the refresh cookie or the password. */
	private static final String[] AUTH_ENTRY_PATHS = { "/api/auth/login", "/api/auth/refresh", "/api/auth/logout" };

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper,
			Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter) throws Exception {
		AuthenticationEntryPoint unauthorized = (req, res, ex) -> writeError(res, objectMapper,
				HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Please log in");
		AccessDeniedHandler forbidden = (req, res, ex) -> {
			if (mustChangePassword()) {
				writeError(res, objectMapper, HttpStatus.FORBIDDEN, "PASSWORD_CHANGE_REQUIRED",
						"Please change your temporary password first");
			}
			else {
				writeError(res, objectMapper, HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission for this action");
			}
		};

		http
			.csrf(AbstractHttpConfigurer::disable)          // bearer tokens; the refresh cookie is SameSite=Strict
			.cors(Customizer.withDefaults())
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.oauth2ResourceServer(o -> o
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
				.authenticationEntryPoint(unauthorized)      // missing / expired / forged token
				.accessDeniedHandler(forbidden))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(PUBLIC_PATHS).permitAll()
				.requestMatchers(HttpMethod.POST, AUTH_ENTRY_PATHS).permitAll()
				.requestMatchers("/api/auth/**").authenticated()   // me, change-password: allowed with a temporary password
				.requestMatchers("/api/admin/**").hasRole("IT_ADMIN")
				.requestMatchers("/api/clinical/**").hasRole("DOCTOR")
				.requestMatchers("/api/staff/**").hasAnyRole("DOCTOR", "RECEPTIONIST", "IT_ADMIN")
				.anyRequest().denyAll())
			.exceptionHandling(e -> e.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden));
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(AppProperties props) {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(props.corsAllowedOrigins());
		cors.addAllowedMethod("*");
		cors.addAllowedHeader("*");
		cors.setAllowCredentials(true);       // refresh-token cookie
		cors.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", cors);
		return source;
	}

	private static boolean mustChangePassword() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth != null && auth.getAuthorities().stream()
			.anyMatch(a -> JwtConfig.PASSWORD_CHANGE_REQUIRED.equals(a.getAuthority()));
	}

	private static void writeError(HttpServletResponse res, ObjectMapper mapper, HttpStatus status, String code,
			String message) throws IOException {
		res.setStatus(status.value());
		res.setContentType(MediaType.APPLICATION_JSON_VALUE);
		mapper.writeValue(res.getOutputStream(), ApiError.of(status.value(), code, message));
	}

}
