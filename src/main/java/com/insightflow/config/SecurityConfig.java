package com.insightflow.config;

import com.insightflow.security.JwtAuthEntryPoint;
import com.insightflow.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            UserDetailsService userDetailsService,
            JwtAuthEntryPoint jwtAuthEntryPoint
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                /*
                 * Enable CORS.
                 *
                 * Spring Security will use the CorsConfigurationSource
                 * bean defined in CorsConfig.java.
                 *
                 * DO NOT define another CorsConfigurationSource bean here.
                 */
                .cors(Customizer.withDefaults())

                /*
                 * Disable CSRF because this backend uses stateless
                 * JWT authentication.
                 */
                .csrf(AbstractHttpConfigurer::disable)

                /*
                 * Return our custom 401 response when authentication fails.
                 */
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthEntryPoint)
                )

                .authorizeHttpRequests(auth -> auth

                        /*
                         * Allow browser CORS preflight requests.
                         */
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        /*
                         * Public authentication endpoints.
                         */
                        .requestMatchers("/auth/**")
                        .permitAll()

                        /*
                         * Public health check endpoint.
                         */
                        .requestMatchers(HttpMethod.GET, "/health")
                        .permitAll()

                        /*
                         * Public tracking script endpoint.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/tracking/script",
                                "/tracking/script/**"
                        )
                        .permitAll()

                        /*
                         * Public analytics ingestion endpoints.
                         *
                         * These endpoints must remain public because tracked
                         * websites send analytics data without dashboard JWTs.
                         *
                         * Project identification/authentication should be
                         * performed using the tracking ID/API key inside the
                         * TrackingService layer.
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/tracking/page-view",
                                "/tracking/event",
                                "/tracking/session-start",
                                "/tracking/session-end"
                        )
                        .permitAll()

                        /*
                         * Public Swagger / OpenAPI documentation.
                         */
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        )
                        .permitAll()

                        /*
                         * User APIs.
                         */
                        .requestMatchers("/users/**")
                        .hasAnyRole("USER", "ADMIN")

                        /*
                         * Admin APIs.
                         */
                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        /*
                         * Everything else requires a valid JWT.
                         *
                         * This protects:
                         * /projects/**
                         * /analytics/**
                         * /events/**
                         * /conversion-goals/**
                         * /funnels/**
                         * /live-activity/**
                         * and future endpoints by default.
                         */
                        .anyRequest()
                        .authenticated()
                )

                /*
                 * Stateless authentication.
                 *
                 * Spring Security will not create HTTP sessions.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                /*
                 * Authentication provider used by login authentication.
                 */
                .authenticationProvider(authenticationProvider())

                /*
                 * Execute JWT validation before Spring Security's
                 * username/password authentication filter.
                 */
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(passwordEncoder());

        provider.setUserDetailsService(userDetailsService);

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}