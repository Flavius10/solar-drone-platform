package com.solar.backend.security;

import com.solar.backend.security.CustomUserDetailsService;
import com.solar.backend.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService, JwtAuthFilter jwtAuthFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/auth/users").hasRole("ADMIN")
                        .requestMatchers("/api/auth/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/farms").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/farms/*/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/farms/*/users/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/farms/*/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/farms/*/defaults").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/farms/*/notification-settings").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/farms/*/pricing-settings").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/farms/*/test-webhook").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/farms/*/branding-settings").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/farms/*/branding-logo").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/farms/*/branding-logo").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/farms/*/branding-logo").hasRole("ADMIN")
                        .requestMatchers("/api/farms/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/panels/bulk-import").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/panels").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/panels/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/inspections/upload").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/inspections/upload-video").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers("/api/inspections/**").authenticated()
                        .requestMatchers("/api/panels/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/work-orders/*/assign").hasAnyRole("ADMIN", "TECHNICIAN")
                        .requestMatchers(HttpMethod.PUT, "/api/work-orders/*/status").hasAnyRole("ADMIN", "TECHNICIAN")
                        .requestMatchers("/api/work-orders/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/audit-log/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}