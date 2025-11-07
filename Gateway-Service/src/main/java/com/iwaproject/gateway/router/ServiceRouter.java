package com.iwaproject.gateway.router;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service Router (DEPRECATED - MVC version).
 * This class is disabled in favor of Spring Cloud Gateway routes.
 * Only active with 'mvc' profile (not used anymore).
 */
@RestController
@Profile("mvc")
@Deprecated
public class ServiceRouter {
    // This class is no longer used with Spring Cloud Gateway
    // See GatewayConfig and GatewayRouteConfig for reactive routing
}
