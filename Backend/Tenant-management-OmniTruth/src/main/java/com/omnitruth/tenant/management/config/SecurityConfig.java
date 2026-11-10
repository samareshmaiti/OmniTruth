
package com.omnitruth.tenant.management.config;

import com.omnitruth.tenant.management.domain.TenantUser;
import com.omnitruth.tenant.management.repository.TenantIdentityMappingRepository;
import com.omnitruth.tenant.management.repository.TenantUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Pattern ENTRA_ISSUER = Pattern.compile("^https://login\\.microsoftonline\\.com/([0-9a-fA-F-]{36})/v2\\.0$");

    private static final String COMPLETE_ONBOARDING_PATH = "/api/public/v1/invitations/complete";

    // SECURITY FILTER CHAIN
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManagerResolver<HttpServletRequest> tenantAuthenticationManagerResolver) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Invitation completion requires a valid Entra JWT.
                        // This endpoint also permits not-yet-mapped Entra tenants.
                        .requestMatchers(HttpMethod.POST, COMPLETE_ONBOARDING_PATH).authenticated()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/tenant/v1/**").hasRole("COMPANY_ADMIN")
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .bearerTokenResolver(publicAwareBearerTokenResolver())
                        .authenticationManagerResolver(tenantAuthenticationManagerResolver)
                );

        return http.build();
    }

    // PUBLIC-AWARE BEARER TOKEN RESOLVER
    @Bean
    BearerTokenResolver publicAwareBearerTokenResolver() {
        DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();

        return request -> {
            String path = request.getServletPath();
            if (path.startsWith("/api/public/") && !COMPLETE_ONBOARDING_PATH.equals(path)) {
                return null;
            }
            return defaultResolver.resolve(request);
        };
    }
    // CORS CONFIGURATION

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${omnitruth.security.allowed-origins:http://localhost:4200}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toList()
        );

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // MULTI-TENANT AUTHENTICATION MANAGER RESOLVER
    @Bean
    AuthenticationManagerResolver<HttpServletRequest> tenantAuthenticationManagerResolver(TenantIdentityMappingRepository mappings, TenantUserRepository tenantUsers, @Value("${omnitruth.security.api-audience}") String apiAudience) {

        Map<String, AuthenticationManager> managerCache = new ConcurrentHashMap<>();
        AuthenticationManagerResolver<String> managerForIssuer = issuer -> {
            Matcher matcher = ENTRA_ISSUER.matcher(issuer);
            if (!matcher.matches()) {
                return null;
            }

            String entraTenantId = matcher.group(1);
            return managerCache.computeIfAbsent(issuer, trustedIssuer -> {
                // Create JWT decoder for the Entra issuer
                NimbusJwtDecoder decoder = NimbusJwtDecoder
                        .withIssuerLocation(trustedIssuer)
                        .build();

                // AUDIENCE VALIDATION
                OAuth2TokenValidator<Jwt> audienceValidator = jwt ->
                        jwt.getAudience().contains(apiAudience)
                                ? OAuth2TokenValidatorResult.success()
                                : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "Unexpected token audience.", null)
                        );

                // -------------------------------------------------
                // TENANT ID VALIDATION
                // -------------------------------------------------

                OAuth2TokenValidator<Jwt> tenantValidator = jwt ->
                        entraTenantId.equalsIgnoreCase(
                                jwt.getClaimAsString("tid")
                        )
                                ? OAuth2TokenValidatorResult.success()
                                : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error(
                                        "invalid_token",
                                        "Token tenant does not match issuer.",
                                        null
                                )
                        );

                // -------------------------------------------------
                // DELEGATED SCOPE VALIDATION
                // -------------------------------------------------

                OAuth2TokenValidator<Jwt> scopeValidator = jwt -> {
                    String scopes = jwt.getClaimAsString("scp");

                    return scopes != null
                            && Arrays.asList(scopes.split(" "))
                            .contains("access_as_user")
                            ? OAuth2TokenValidatorResult.success()
                            : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error(
                                    "insufficient_scope",
                                    "Required delegated scope is missing.",
                                    null
                            )
                    );
                };

                // -------------------------------------------------
                // COMBINE JWT VALIDATORS
                // -------------------------------------------------

                decoder.setJwtValidator(
                        new DelegatingOAuth2TokenValidator<>(
                                JwtValidators.createDefaultWithIssuer(trustedIssuer),
                                audienceValidator,
                                tenantValidator,
                                scopeValidator
                        )
                );

                // -------------------------------------------------
                // JWT AUTHENTICATION PROVIDER
                // -------------------------------------------------

                JwtAuthenticationProvider provider =
                        new JwtAuthenticationProvider(decoder);

                provider.setJwtAuthenticationConverter(
                        jwtAuthenticationConverter(tenantUsers)
                );

                return new ProviderManager(provider);
            });
        };

        // -----------------------------------------------------
        // NORMAL APPLICATION API RESOLVER
        // -----------------------------------------------------
        // Only active and already-mapped Entra tenants are allowed.

        JwtIssuerAuthenticationManagerResolver mappedTenantResolver =
                new JwtIssuerAuthenticationManagerResolver(issuer -> {

                    Matcher matcher = ENTRA_ISSUER.matcher(issuer);

                    if (!matcher.matches()) {
                        return null;
                    }

                    String entraTenantId = matcher.group(1);

                    if (!mappings.existsByEntraTenantIdIgnoreCaseAndActiveTrue(
                            entraTenantId
                    )) {
                        return null;
                    }

                    // Use resolve(), not apply().
                    return managerForIssuer.resolve(issuer);
                });

        // -----------------------------------------------------
        // INVITATION COMPLETION RESOLVER
        // -----------------------------------------------------
        // Allows valid but not-yet-mapped Entra tenants
        // to authenticate for invitation completion.

        JwtIssuerAuthenticationManagerResolver onboardingResolver =
                new JwtIssuerAuthenticationManagerResolver(managerForIssuer);

        // -----------------------------------------------------
        // SELECT RESOLVER BASED ON REQUEST PATH
        // -----------------------------------------------------

        return request ->
                COMPLETE_ONBOARDING_PATH.equals(request.getServletPath())
                        ? onboardingResolver.resolve(request)
                        : mappedTenantResolver.resolve(request);
    }

    // ---------------------------------------------------------
    // JWT AUTHENTICATION CONVERTER
    // ---------------------------------------------------------

    private JwtAuthenticationConverter jwtAuthenticationConverter(TenantUserRepository tenantUsers) {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> authorities = new LinkedHashSet<>();
            // SCOPE AUTHORITIES
            Collection<GrantedAuthority> scopeAuthorities = scopes.convert(jwt);
            if (scopeAuthorities != null) {
                authorities.addAll(scopeAuthorities);
            }

            // -------------------------------------------------
            // ENTRA APP ROLES
            // -------------------------------------------------
            // Preserve existing Entra app-role claims.

            List<String> entraRoles = jwt.getClaimAsStringList("roles");
            if (entraRoles != null) {
                entraRoles.stream()
                        .filter(Objects::nonNull)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .forEach(authorities::add);
            }

            // -------------------------------------------------
            // OMNITRUTH MANAGED ROLES
            // -------------------------------------------------
            // Resolve Company Admin role using immutable
            // Entra identity: tenant ID + object ID.

            String entraTenantId = jwt.getClaimAsString("tid");
            String entraObjectId = jwt.getClaimAsString("oid");
            if (entraTenantId != null && entraObjectId != null) {
                tenantUsers.findByEntraTenantIdIgnoreCaseAndEntraObjectIdIgnoreCaseAndStatus(entraTenantId, entraObjectId, "ACTIVE")
                        .map(TenantUser::getAppRole)
                        .filter("COMPANY_ADMIN"::equals)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .ifPresent(authorities::add);
            }


            System.out.println("========== JWT AUTHORIZATION DEBUG ==========");
            System.out.println("JWT issuer: " + jwt.getIssuer());
            System.out.println("JWT audience: " + jwt.getAudience());
            System.out.println("JWT scope: " + jwt.getClaimAsString("scp"));
            System.out.println("JWT tenant: " + jwt.getClaimAsString("tid"));
            System.out.println("JWT object: " + jwt.getClaimAsString("oid"));

            authorities.forEach(authority ->
                    System.out.println(
                            "Granted authority: " + authority.getAuthority()
                    )
            );

            System.out.println("=============================================");

            return authorities;
        });

        return converter;
    }
}
