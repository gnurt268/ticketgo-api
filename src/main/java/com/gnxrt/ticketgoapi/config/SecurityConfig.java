package com.gnxrt.ticketgoapi.config;

import com.gnxrt.ticketgoapi.security.JwtAuthenticationFilter;
import com.gnxrt.ticketgoapi.security.CustomAuthenticationEntryPoint;
import com.gnxrt.ticketgoapi.security.CustomAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authManagerBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configure(http))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/events/**", "/api/categories/**").permitAll()
                        .requestMatchers("/api/payment/vnpay/return", "/api/payment/vnpay/ipn").permitAll()
                        .requestMatchers("/api/qrcode/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/event/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/waiting-room/event/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/waiting-room/slug/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/zones/**").permitAll()
                        .requestMatchers("/api/orders/**").hasAnyRole("USER", "ORGANIZER", "ADMIN")
                        .requestMatchers("/api/tickets/**").hasAnyRole("USER", "ORGANIZER", "ADMIN")
                        .requestMatchers("/api/reviews/**").hasAnyRole("USER", "ORGANIZER", "ADMIN")
                        .requestMatchers("/api/waiting-room/**").authenticated()
                        .requestMatchers("/api/organizer/**").hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}