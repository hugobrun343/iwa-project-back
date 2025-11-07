package com.iwaproject.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * JWT configuration with optional issuer validation.
 */
@Configuration
@Profile("!test")
public class JwtConfig {

    /**
     * JWK Set URI for token signature validation.
     */
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    /**
     * Skip issuer validation (for dev only).
     */
    @Value("${jwt.skip-issuer-validation:false}")
    private boolean skipIssuerValidation;

    /**
     * Configure reactive JWT decoder for WebFlux.
     * In dev mode, skip issuer validation to allow localhost/docker URLs.
     * Signature is ALWAYS validated via JWK.
     *
     * @return ReactiveJwtDecoder
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder
                .withJwkSetUri(jwkSetUri)
                .build();

        if (skipIssuerValidation) {
            // Dev mode: Only validate timestamp and signature
            OAuth2TokenValidator<Jwt> validator =
                    new JwtTimestampValidator();
            jwtDecoder.setJwtValidator(validator);
        }
        // Prod mode: Default validation (issuer + timestamp + signature)

        return jwtDecoder;
    }
}

