# Stage 1: Build all Keycloakify themes
FROM node:20-alpine AS theme-builder

RUN apk add --no-cache maven

WORKDIR /app

# Copy each theme directory explicitly
COPY Keycloak-Service/keycloakify-theme-homeguard ./keycloakify-theme-homeguard
COPY Keycloak-Service/build-all-themes.sh ./

# Ensure Unix line endings and execute the build script
RUN sed -i 's/\r$//' build-all-themes.sh \
	&& chmod +x build-all-themes.sh \
	&& ./build-all-themes.sh

# Stage 2: Build Keycloak with custom themes
FROM quay.io/keycloak/keycloak:latest AS builder

# Configure build-time options via environment variables
ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

WORKDIR /opt/keycloak

COPY --from=theme-builder --chown=keycloak:keycloak --chmod=644 /app/built-themes/*.jar /opt/keycloak/providers/
RUN touch -m --date=@1743465600 /opt/keycloak/providers/*

# Build Keycloak with the configured options
RUN /opt/keycloak/bin/kc.sh build

# Stage 3: Final runtime image
FROM quay.io/keycloak/keycloak:latest

COPY --from=builder /opt/keycloak/ /opt/keycloak/

WORKDIR /opt/keycloak

# Set the same build-time options for runtime consistency
ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Set the entrypoint and default command
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized"]