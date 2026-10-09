package com.bilangieri.agendamento.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import jakarta.servlet.DispatcherType;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                // 401/403 barrados aqui seguem o mesmo formato (Problem Details) do GlobalExceptionHandler
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, e) ->
                                handlerExceptionResolver.resolveException(request, response, null, e)
                        )
                        .accessDeniedHandler((request, response, e) ->
                                handlerExceptionResolver.resolveException(request, response, null, e)
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/register/professional",
                                "/api/auth/login"
                        ).permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Administração (aprovação de prestadores)
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // Customers
                        .requestMatchers(HttpMethod.GET, "/api/customers/me")
                        .hasRole("CUSTOMER")
                        .requestMatchers("/api/customers", "/api/customers/**")
                        .hasRole("ADMIN")

                        // Categorias: todos consultam, só o ADMIN gere (anyRequest)
                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**")
                        .hasAnyRole("ADMIN", "CUSTOMER", "PROFESSIONAL")

                        // Horário de trabalho dos prestadores
                        .requestMatchers(HttpMethod.PUT, "/api/professionals/me/working-hours")
                        .hasRole("PROFESSIONAL")
                        .requestMatchers(HttpMethod.GET, "/api/professionals/*/working-hours")
                        .hasAnyRole("ADMIN", "CUSTOMER", "PROFESSIONAL")

                        // Services (inclui /{id}/availability)
                        .requestMatchers(HttpMethod.GET, "/api/services", "/api/services/**")
                        .hasAnyRole("ADMIN", "CUSTOMER", "PROFESSIONAL")
                        .requestMatchers("/api/services", "/api/services/**")
                        .hasAnyRole("ADMIN", "PROFESSIONAL")

                        // Appointments
                        .requestMatchers(HttpMethod.PATCH, "/api/appointments/*/cancel")
                        .hasAnyRole("ADMIN", "CUSTOMER", "PROFESSIONAL")
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/appointments/*/confirm",
                                "/api/appointments/*/complete",
                                "/api/appointments/*/no-show"
                        ).hasAnyRole("ADMIN", "PROFESSIONAL")
                        .requestMatchers(HttpMethod.POST, "/api/appointments")
                        .hasAnyRole("ADMIN", "CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/appointments", "/api/appointments/**")
                        .hasAnyRole("ADMIN", "CUSTOMER", "PROFESSIONAL")
                        .requestMatchers("/api/appointments", "/api/appointments/**")
                        .hasRole("ADMIN")

                        .anyRequest().hasRole("ADMIN")
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}