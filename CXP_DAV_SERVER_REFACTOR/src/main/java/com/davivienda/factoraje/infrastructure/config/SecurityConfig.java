package com.davivienda.factoraje.infrastructure.config;

import com.davivienda.factoraje.domain.constants.Roles;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.infrastructure.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final SystemParameters systemParameters;
    private final ObjectMapper objectMapper;

    /**
     * Las reglas se evalúan en orden: las rutas específicas van antes que el
     * comodín de su controlador. Todo endpoint no listado se deniega. La
     * propiedad de los datos (que un proveedor o pagador solo vea su entidad) se
     * valida en {@code CurrentUserService}.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/sso/handoff", "/api/v1/sso/activateSession").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/v1/term-versions/active/supplier").hasRole(Roles.SUPPLIER)
                        .requestMatchers(HttpMethod.GET, "/api/v1/term-versions/active/payer").hasAnyRole(Roles.PAYER, Roles.ADMIN)
                        .requestMatchers("/api/v1/term-versions/**").hasRole(Roles.ADMIN)

                        .requestMatchers("/api/v1/parameters/**").hasRole(Roles.SYSTEM_ADMIN)

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/master-agreements/supplier/*",
                                "/api/v1/master-agreements/*/next-disbursement-date").hasRole(Roles.SUPPLIER)
                        .requestMatchers("/api/v1/master-agreements/**").hasRole(Roles.ADMIN)

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/documents/master-agreement/*/financeable",
                                "/api/v1/documents/supplier/*/history").hasRole(Roles.SUPPLIER)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/documents/payer/*/history",
                                "/api/v1/documents/payer/*/history/summary").hasAnyRole(Roles.PAYER, Roles.ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/documents/*/inactivate").hasRole(Roles.PAYER)

                        .requestMatchers(HttpMethod.GET, "/api/v1/payers/me/summary").hasRole(Roles.PAYER)
                        .requestMatchers(HttpMethod.GET, "/api/v1/payers/*/summary").hasRole(Roles.ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/suppliers/me/bank-account").hasRole(Roles.SUPPLIER)

                        .requestMatchers(HttpMethod.POST, "/api/v1/funding-requests/calculate",
                                "/api/v1/funding-requests/submit").hasRole(Roles.SUPPLIER)

                        .requestMatchers(HttpMethod.POST, "/api/v1/batches/upload").hasAnyRole(Roles.PAYER, Roles.ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/batches/upload-settings").hasAnyRole(Roles.PAYER, Roles.ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/upload-resources",
                                "/api/v1/upload-resources/template", "/api/v1/upload-resources/manual")
                                .hasAnyRole(Roles.PAYER, Roles.ADMIN)
                        .requestMatchers("/api/v1/upload-resources/**").hasRole(Roles.ADMIN)

                        .requestMatchers(
                                "/api/v1/disbursement-requests/**",
                                "/api/v1/dispersion-requests/**",
                                "/api/v1/entities/**",
                                "/api/v1/users/**",
                                "/api/v1/roles/**",
                                "/api/v1/credit-facilities/**",
                                "/api/v1/product-pricing-terms/**",
                                "/api/v1/disbursement-policies/**",
                                "/api/v1/payment-policies/**",
                                "/api/v1/holidays/**").hasRole(Roles.ADMIN)

                        .anyRequest().denyAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> writeError(response,
                                HttpServletResponse.SC_UNAUTHORIZED, "La sesión no es válida o expiró."))
                        .accessDeniedHandler((request, response, e) -> writeError(response,
                                HttpServletResponse.SC_FORBIDDEN, "No tiene permisos para realizar esta operación.")))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(message));
    }

    /**
     * Los orígenes se leen del parámetro CORS_ALLOWED_ORIGINS en cada petición
     * (con la caché de {@link SystemParameters}), así que un cambio no requiere
     * reiniciar.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(systemParameters.getList(SystemParameterKey.CORS_ALLOWED_ORIGINS));
            configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
            configuration.setAllowCredentials(true);
            return configuration;
        };
    }
}
