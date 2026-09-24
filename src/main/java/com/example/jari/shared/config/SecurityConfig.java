package com.example.jari.shared.config;

import com.example.jari.shared.security.JwtAuthenticationFilter;
import com.example.jari.user.oauth2.OAuth2AuthenticationFailureHandler;
import com.example.jari.user.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final AppProperties appProperties;
    private final ObjectProvider<OAuth2AuthenticationSuccessHandler> oAuth2SuccessHandlerProvider;
    private final ObjectProvider<OAuth2AuthenticationFailureHandler> oAuth2FailureHandlerProvider;

    private static final String[] PUBLIC_PATHS = {
        "/api/v1/auth/**",
        "/oauth2/**",
        "/login/oauth2/**",
        "/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/actuator/health",
        "/actuator/info",
        "/actuator/prometheus",
        "/actuator/metrics",
        // GitHub App webhooks (HMAC verified) + post-install setup callback (state signed)
        "/api/v1/webhooks/github",
        "/api/v1/github/setup",
        // WebSocket endpoint stays open at HTTP level; real auth happens on the
        // STOMP CONNECT frame (WebSocketAuthChannelInterceptor).
        "/ws/**",
        "/ws"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean oauth2Enabled = appProperties.getSecurity().isOauth2Enabled();

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            );

        if (oauth2Enabled) {
            OAuth2AuthenticationSuccessHandler successHandler = oAuth2SuccessHandlerProvider.getIfAvailable();
            OAuth2AuthenticationFailureHandler failureHandler = oAuth2FailureHandlerProvider.getIfAvailable();
            if (successHandler != null && failureHandler != null) {
                http.oauth2Login(oauth2 -> oauth2
                    .authorizationEndpoint(ep -> ep.baseUri("/oauth2/authorize"))
                    .successHandler(successHandler)
                    .failureHandler(failureHandler)
                );
            }
        }

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
