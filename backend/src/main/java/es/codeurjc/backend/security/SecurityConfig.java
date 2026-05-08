package es.codeurjc.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import es.codeurjc.backend.security.jwt.JwtRequestFilter;
import es.codeurjc.backend.security.jwt.UnauthorizedHandlerJwt;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	// API ENDPOINTS
    private static final String API_V = "/api/v1";
    private static final String API_POSTS = API_V + "/posts/**";
    private static final String API_USERS = API_V + "/users/**";
	private static final String API_IMAGES = API_V + "/images/**";

	private final JwtRequestFilter jwtRequestFilter;
	private final RepositoryUserDetailsService userDetailsService;
	private final UnauthorizedHandlerJwt unauthorizedHandlerJwt;

	public SecurityConfig(
			JwtRequestFilter jwtRequestFilter,
			RepositoryUserDetailsService userDetailsService,
			UnauthorizedHandlerJwt unauthorizedHandlerJwt
	) {
		this.jwtRequestFilter = jwtRequestFilter;
		this.userDetailsService = userDetailsService;
		this.unauthorizedHandlerJwt = unauthorizedHandlerJwt;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}

	@Bean
	public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
		
		http.authenticationProvider(authenticationProvider());

		http
			.securityMatcher("/api/**")
			.cors(Customizer.withDefaults())
			.exceptionHandling(handling -> handling.authenticationEntryPoint(unauthorizedHandlerJwt));
		
		http
			.authorizeHttpRequests(authorize -> authorize
					// POSTS
					.requestMatchers(HttpMethod.GET, API_POSTS).permitAll()
					.requestMatchers(HttpMethod.POST, API_POSTS).hasRole("USER")
					.requestMatchers(HttpMethod.PUT, API_POSTS).hasRole("USER")
					.requestMatchers(HttpMethod.DELETE, API_POSTS).hasRole("USER")
					
					// IMAGES
					.requestMatchers(HttpMethod.GET, API_IMAGES).permitAll()
					.requestMatchers(HttpMethod.POST, API_IMAGES).hasRole("USER")
					.requestMatchers(HttpMethod.DELETE, API_IMAGES).hasRole("USER")
					
					// USERS
					.requestMatchers(HttpMethod.GET, API_USERS).permitAll()
					.requestMatchers(HttpMethod.POST, API_USERS).permitAll()
					.requestMatchers(HttpMethod.PATCH, API_USERS).hasRole("USER")
					.requestMatchers(HttpMethod.DELETE, API_USERS).hasRole("USER")
					
					// AUTH
					.requestMatchers(HttpMethod.GET, API_V + "/auth/me").hasRole("USER")
					
					// DOCUMENTATION
					.requestMatchers("/v3/api-docs/**").permitAll()
					.requestMatchers("/swagger-ui.html").permitAll()
					.requestMatchers("/swagger-ui/**").permitAll()
					.requestMatchers("/v3/api-docs.yaml").permitAll()
					.requestMatchers("/api-docs/**").permitAll()
					
					// Otros
					.anyRequest().permitAll()
			);
		
        // Disable Form login Authentication
        http.formLogin(AbstractHttpConfigurer::disable);

        // Disable CSRF protection (it is difficult to implement in REST APIs)
        http.csrf(AbstractHttpConfigurer::disable);

        // Disable Basic Authentication
        http.httpBasic(AbstractHttpConfigurer::disable);

        // Stateless session
        http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		// Add JWT Token filter
		http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

}