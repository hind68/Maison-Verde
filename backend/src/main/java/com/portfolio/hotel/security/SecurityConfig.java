package com.portfolio.hotel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.hotel.shared.error.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(
            HotelUserDetailsService userDetailsService, PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, ObjectMapper objectMapper, SecurityContextRepository contextRepository,
            PasswordChangeRequiredFilter passwordChangeRequiredFilter
    ) throws Exception {
        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookiePath("/");
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        csrfRequestHandler.setCsrfRequestAttributeName(null);

        http
                .cors(cors -> { })
                .csrf(configurer -> configurer
                        .csrfTokenRepository(csrf)
                        .csrfTokenRequestHandler(csrfRequestHandler))
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/**").authenticated()
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/rooms/*/housekeeping")
                            .hasAnyRole("ADMIN", "MANAGER", "HOUSEKEEPING")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/rooms/*/operational-status")
                            .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/rooms", "/api/v1/rooms/**")
                            .hasAnyRole("ADMIN", "MANAGER", "RECEPTION", "HOUSEKEEPING")
                        .requestMatchers("/api/v1/clients/**", "/api/v1/reservations/**", "/api/v1/planning/**")
                            .hasAnyRole("ADMIN", "MANAGER", "RECEPTION")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> writeError(
                                response, objectMapper, 401, "AUTHENTICATION_REQUIRED",
                                "Une authentification est requise."))
                        .accessDeniedHandler((request, response, exception) -> writeError(
                                response, objectMapper, 403, "ACCESS_DENIED",
                                "Vous n'avez pas l'autorisation nécessaire.")))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());
        http.addFilterAfter(passwordChangeRequiredFilter, AnonymousAuthenticationFilter.class);
        return http.build();
    }

    private void writeError(
            HttpServletResponse response, ObjectMapper objectMapper, int status, String code, String message
    ) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiError.of(status, code, message));
    }
}
