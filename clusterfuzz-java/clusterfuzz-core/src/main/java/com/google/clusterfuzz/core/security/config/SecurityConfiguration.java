package com.google.clusterfuzz.core.security.config;

import com.google.clusterfuzz.core.security.authentication.JwtAuthenticationFilter;
import com.google.clusterfuzz.core.security.authentication.OAuth2AuthenticationProvider;
import com.google.clusterfuzz.core.security.authorization.CustomPermissionEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Main security configuration for ClusterFuzz.
 * Configures authentication, authorization, and security policies.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2AuthenticationProvider oAuth2AuthenticationProvider;
    private final CustomPermissionEvaluator permissionEvaluator;

    /**
     * Main security filter chain configuration.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for API endpoints
            .csrf(csrf -> csrf.disable())
            
            // Configure session management
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/health/**").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                
                // Admin endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/config/**").hasRole("ADMIN")
                
                // Bot endpoints
                .requestMatchers("/api/v1/bot/**").hasRole("BOT")
                .requestMatchers("/api/v1/tasks/**").hasAnyRole("BOT", "ADMIN")
                
                // User endpoints
                .requestMatchers("/api/v1/testcases/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/v1/issues/**").hasAnyRole("USER", "ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Add JWT filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Configure exception handling
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            );

        return http.build();
    }

    /**
     * Password encoder for user passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Method security expression handler with custom permission evaluator.
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }

    /**
     * Custom authentication entry point for API responses.
     */
    @Bean
    public org.springframework.security.web.AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage() + "\"}"
            );
        };
    }

    /**
     * Custom access denied handler for API responses.
     */
    @Bean
    public org.springframework.security.web.access.AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(403);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Forbidden\",\"message\":\"" + accessDeniedException.getMessage() + "\"}"
            );
        };
    }
}