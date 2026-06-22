package badamoyeo_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import badamoyeo_api.auth.security.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {
	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.cors(Customizer.withDefaults())
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(
					"/auth/signup",
					"/auth/login",
					"/auth/refresh",
					"/auth/logout",
					"/auth/oauth/**",
					"/dashboard/**",
					"/spots",
					"/spots/*",
					"/uploads/**"
				).permitAll()
				.requestMatchers(HttpMethod.GET, "/ai/spot-recommendations").permitAll()
				.requestMatchers(HttpMethod.GET, "/spots/*/ai-analysis").permitAll()
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.GET, "/spots/*/posts").permitAll()
				.requestMatchers(HttpMethod.GET, "/posts/*").permitAll()
				.requestMatchers(HttpMethod.GET, "/posts/*/comments").permitAll()
				.requestMatchers(
					"/users/me/**",
					"/spots/*/favorite",
					"/posts/images",
					"/spots/*/posts",
					"/posts/*/likes",
					"/posts/*/comments",
					"/comments/*"
				).authenticated()
				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
