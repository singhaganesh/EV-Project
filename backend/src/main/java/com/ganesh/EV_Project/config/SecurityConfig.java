package com.ganesh.EV_Project.config;

import com.ganesh.EV_Project.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // âœ… Enable @PreAuthorize
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow Localhost and your production domain
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173", 
            "https://plugsy.in", 
            "http://plugsy.in",
            "https://dashboard.plugsy.in",
            "https://plugsypartner.netlify.app",
            "https://partner.plugsy.in"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Cache preflight for 1 hour
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "Access-Control-Allow-Origin"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // Public Auth Endpoints
                    auth.requestMatchers("/api/auth/**").permitAll();
                    
                    // Station Discovery - GET is public, Mutations are SECURE
                    auth.requestMatchers(HttpMethod.GET, "/api/stations/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/dispensaries/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/slots/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/iot/**").permitAll();
                    
                    // Public System Endpoints
                    auth.requestMatchers("/api/public/**").permitAll();
                    auth.requestMatchers("/ws/**").permitAll();
                    auth.requestMatchers("/api/payments/webhook").permitAll();
                    auth.requestMatchers("/h2-console/**").permitAll();
                    auth.requestMatchers("/error").permitAll();
                    
                    // Everything else requires Authentication
                    auth.anyRequest().authenticated();
                });

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
