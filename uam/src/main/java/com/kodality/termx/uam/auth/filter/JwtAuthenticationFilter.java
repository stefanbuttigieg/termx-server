package com.kodality.termx.uam.auth.filter;

import com.kodality.termx.core.auth.SessionInfo;
import com.kodality.termx.core.auth.SessionStore;
import com.kodality.termx.uam.auth.service.JwtService;
import com.kodality.termx.uam.user.model.User;
import com.kodality.termx.uam.user.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Filter("/**")
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements HttpServerFilter {
    
    private final JwtService jwtService;
    private final UserService userService;
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1; // Run before AuthorizationFilter
    }
    
    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        try {
            Optional<String> tokenOptional = extractTokenFromRequest(request);
            
            if (tokenOptional.isPresent()) {
                String token = tokenOptional.get();
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
                    }
                }
            }
        } catch (JwtException e) {
            log.debug("Invalid JWT token", e);
        } catch (Exception e) {
            log.error("Error processing JWT authentication", e);
        }
        
        return chain.proceed(request);
    }
    
    private Optional<String> extractTokenFromRequest(HttpRequest<?> request) {
        return request.getHeaders().getAuthorization()
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7));
    }
}
