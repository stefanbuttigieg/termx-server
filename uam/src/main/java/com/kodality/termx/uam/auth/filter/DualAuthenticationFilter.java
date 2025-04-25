package com.kodality.termx.uam.auth.filter;

import com.kodality.termx.core.auth.SessionInfo;
import com.kodality.termx.core.auth.SessionStore;
import com.kodality.termx.uam.auth.service.JwtService;
import com.kodality.termx.uam.user.model.User;
import com.kodality.termx.uam.user.service.UserService;
import io.jsonwebtoken.JwtException;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.reactivex.rxjava3.core.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;

import java.util.Optional;
import java.util.Set;

/**
 * Dual authentication filter that supports both Keycloak and JWT authentication.
 * This allows for a smooth transition from Keycloak to our custom authentication system.
 */
@Slf4j
@Filter("/**")
@RequiredArgsConstructor
public class DualAuthenticationFilter implements HttpServerFilter {
    
    private final JwtService jwtService;
    private final UserService userService;
    
    @Value("${auth.dual-mode:true}")
    private boolean dualMode;
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1; // Run before AuthorizationFilter
    }
    
    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        try {
            // First, check for JWT token
            Optional<String> jwtTokenOptional = extractJwtTokenFromRequest(request);
            
            if (jwtTokenOptional.isPresent()) {
                String token = jwtTokenOptional.get();
                String username = jwtService.extractUsername(token);
                
                if (username != null && !jwtService.isTokenExpired(token)) {
                    Optional<User> userOptional = userService.findByUsername(username);
                    
                    if (userOptional.isPresent()) {
                        User user = userOptional.get();
                        
                        // Get user privileges
                        Set<String> privileges = userService.getUserPrivileges(user.getId());
                        
                        // Create session info
                        SessionInfo sessionInfo = jwtService.createSessionInfo(token, username, privileges);
                        
                        // Add session info to request attributes
                        request.setAttribute(SessionStore.KEY, sessionInfo);
                        
                        // Continue with the request
                        return chain.proceed(request);
                    }
                }
            }
            
            // If dual mode is enabled and JWT authentication failed, let the request continue
            // The existing Keycloak filter will handle authentication if applicable
            if (dualMode) {
                return chain.proceed(request);
            }
            
            // If dual mode is disabled and JWT authentication failed, continue without authentication
            // The AuthorizationFilter will handle access control
            return chain.proceed(request);
        } catch (JwtException e) {
            log.debug("Invalid JWT token", e);
            
            // If dual mode is enabled, let the request continue for Keycloak to handle
            if (dualMode) {
                return chain.proceed(request);
            }
            
            // Otherwise, continue without authentication
            return chain.proceed(request);
        } catch (Exception e) {
            log.error("Error processing authentication", e);
            return chain.proceed(request);
        }
    }
    
    private Optional<String> extractJwtTokenFromRequest(HttpRequest<?> request) {
        return request.getHeaders().getAuthorization()
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7));
    }
}
