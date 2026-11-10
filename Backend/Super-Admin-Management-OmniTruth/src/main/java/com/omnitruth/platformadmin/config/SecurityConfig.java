package com.omnitruth.platformadmin.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.omnitruth.platformadmin.tenant.repository.PlatformAdminIdentityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String CONSUMER_TENANT_ID = "9188040d-6c67-4c5b-b112-36a304b66dad";

    @Value("${omnitruth.security.required-audience}")
    private String requiredAudience;

    @Value("${omnitruth.security.platform-tenant-id}")
    private String platformTenantId;

    private final PlatformAdminIdentityRepository platformAdminIdentityRepository;

    public SecurityConfig(PlatformAdminIdentityRepository platformAdminIdentityRepository) {
        this.platformAdminIdentityRepository = platformAdminIdentityRepository;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/api/platform/v1/**").hasRole("PLATFORM_SUPER_ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        // Microsoft common JWKS verifies signatures for both organizational and consumer accounts.
        // The custom validator below still constrains issuer, tenant, audience and delegated scope.
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri("https://login.microsoftonline.com/common/discovery/v2.0/keys")
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();

        OAuth2TokenValidator<Jwt> claimsValidator = jwt -> {
            String tid = jwt.getClaimAsString("tid");
            String issuer = jwt.getClaimAsString("iss");
            String version = jwt.getClaimAsString("ver");
            String scope = jwt.getClaimAsString("scp");

            boolean validTenantAndIssuer = false;
            if (tid != null && issuer != null) {
                try {
                    UUID.fromString(tid);
                    String expectedIssuer = "https://login.microsoftonline.com/" + tid + "/v2.0";
                    validTenantAndIssuer = expectedIssuer.equals(issuer);
                } catch (IllegalArgumentException ignored) {
                    validTenantAndIssuer = false;
                }
            }

            boolean validAudience = jwt.getAudience() != null
                    && jwt.getAudience().contains(requiredAudience);
            boolean validScope = scope != null
                    && List.of(scope.split("\\s+")).contains("access_as_user");
            boolean validVersion = "2.0".equals(version);

            if (validTenantAndIssuer && validAudience && validScope && validVersion) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token",
                    "Token issuer, tenant, audience, version, or delegated scope is invalid",
                    null));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), claimsValidator));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(JdbcTemplate jdbcTemplate) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> platformAuthorities(jwt));
        return converter;
    }

    private Collection<GrantedAuthority> platformAuthorities(Jwt jwt) {

        List<GrantedAuthority> authorities = new ArrayList<>();
        String tenantId = jwt.getClaimAsString("tid");

        // Organizational OmniTruth administrators
        if (platformTenantId.equalsIgnoreCase(tenantId)) {
            List<String> roles = jwt.getClaimAsStringList("roles");

            if (roles != null && roles.stream().anyMatch(role ->
                    "PLATFORM_SUPER_ADMIN".equals(role)
                            || "PLATFORM_SUPER_ADMIN_USER".equals(role))) {

                authorities.add(
                        new SimpleGrantedAuthority("ROLE_PLATFORM_SUPER_ADMIN"));
            }
        }

        // Consumer accounts
        if (CONSUMER_TENANT_ID.equalsIgnoreCase(tenantId)) {

            String issuer = jwt.getClaimAsString("iss");
            String subject = jwt.getSubject();

            if (issuer != null && subject != null) {

                boolean registered =
                        platformAdminIdentityRepository
                                .existsByIssuerAndSubjectAndTenantIdAndAppRoleAndActiveTrue(
                                        issuer,
                                        subject,
                                        UUID.fromString(CONSUMER_TENANT_ID),
                                        "PLATFORM_SUPER_ADMIN"
                                );

                if (registered) {
                    authorities.add(
                            new SimpleGrantedAuthority("ROLE_PLATFORM_SUPER_ADMIN"));
                }
            }
        }

        return authorities;
    }
}