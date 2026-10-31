
package com.omnitruth.platformadmin.config;

import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${omnitruth.security.issuer-uri}")
    String issuerUri;

    @Value("${omnitruth.security.required-audience}")
    String requiredAudience;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

        return http

                // This configuration assumes Bearer-token authentication,
                // not browser-cookie authentication.
                .csrf(csrf -> csrf.disable())

                // Do not create HTTP sessions for this API.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Define endpoint authorization.
                .authorizeHttpRequests(auth -> auth

                        // Public monitoring endpoints.
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Only platform Super Admins can access
                        // platform administration endpoints.
                        .requestMatchers("/api/platform/v1/**")
                        .hasRole("PLATFORM_SUPER_ADMIN")

                        // All other endpoints require authentication.
                        .anyRequest().authenticated()
                )

                // Enable JWT authentication.
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                )

                .build();
    }


    // =========================================
    // 2. JWT DECODER
    // =========================================

    @Bean
    JwtDecoder jwtDecoder(


    ) {

        // Create Nimbus JWT decoder using Microsoft Entra OIDC metadata.
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withIssuerLocation(issuerUri).build();


        // Validate token issuer, signature and timestamps.
        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(issuerUri);


        // Validate that the token was issued for OmniTruth Platform API.
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {

            if (jwt.getAudience() != null
                    && jwt.getAudience().contains(requiredAudience)) {

                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                            "invalid_token",
                            "Required API audience is missing",
                            null
                    )
            );
        };


        // Apply both validators.
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        issuerValidator,
                        audienceValidator
                )
        );

        return decoder;
    }


    // =========================================
    // 3. JWT AUTHENTICATION CONVERTER
    // =========================================

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        // Convert Microsoft Entra application roles
        // into Spring Security authorities.
        converter.setJwtGrantedAuthoritiesConverter(
                this::platformAuthorities
        );

        return converter;
    }


    // =========================================
    // 4. PLATFORM ROLE MAPPING
    // =========================================

    private Collection<GrantedAuthority> platformAuthorities(Jwt jwt) {

        // Microsoft Entra ID provides application roles
        // in the "roles" JWT claim.
        List<String> roles = jwt.getClaimAsStringList("roles");

        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        return roles.stream()

                .filter(role -> role != null && !role.isBlank())

                .map(String::trim)

                // Accept only the exact platform Super Admin role.
                .filter("PLATFORM_SUPER_ADMIN"::equals)

                // Convert to Spring Security authority.
                .map(role -> new SimpleGrantedAuthority(
                        "ROLE_PLATFORM_SUPER_ADMIN"
                ))

                .distinct()
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
