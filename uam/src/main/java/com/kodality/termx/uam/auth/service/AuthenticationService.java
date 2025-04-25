package com.kodality.termx.uam.auth.service;

import com.kodality.commons.exception.BadRequestException;
import com.kodality.commons.exception.ForbiddenException;
import com.kodality.commons.exception.NotFoundException;
import com.kodality.termx.core.auth.SessionInfo;
import com.kodality.termx.core.auth.SessionStore;
import com.kodality.termx.uam.auth.model.AuthProvider;
import com.kodality.termx.uam.auth.model.RefreshToken;
import com.kodality.termx.uam.auth.repository.AuthProviderRepository;
import com.kodality.termx.uam.auth.repository.RefreshTokenRepository;
import com.kodality.termx.uam.user.model.ExternalIdentity;
import com.kodality.termx.uam.user.model.User;
import com.kodality.termx.uam.user.model.UserStatus;
import com.kodality.termx.uam.user.service.UserService;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Singleton
@RequiredArgsConstructor
public class AuthenticationService {
    
    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthProviderRepository authProviderRepository;
    
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        User user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));
        
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("User account is not active");
        }
        
        if (user.getPasswordHash() == null || !userService.validatePassword(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid username or password");
        }
        
        return generateAuthenticationResponse(user);
    }
    
    @Transactional
    public AuthenticationResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        if (username == null) {
            throw new BadRequestException("Invalid refresh token");
        }
        
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BadRequestException("Refresh token not found"));
        
        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Refresh token is expired or revoked");
        }
        
        if (!storedToken.getUserId().equals(user.getId())) {
            throw new BadRequestException("Refresh token does not belong to this user");
        }
        
        return generateAuthenticationResponse(user);
    }
    
    @Transactional
    public void logout(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BadRequestException("Refresh token not found"));
        
        token.setRevoked(true);
        token.setSysModifiedAt(LocalDateTime.now());
        token.setSysModifiedBy("system");
        token.setSysVersion(token.getSysVersion() + 1);
        
        refreshTokenRepository.update(token);
        
        // Clear session
        SessionStore.clearLocal();
    }
    
    @Transactional
    public AuthenticationResponse authenticateWithExternalProvider(String providerId, String externalUserId, String email, String displayName) {
        // Verify provider exists and is enabled
        AuthProvider provider = authProviderRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BadRequestException("Unknown identity provider: " + providerId));
        
        if (!provider.isEnabled()) {
            throw new BadRequestException("Identity provider is disabled: " + providerId);
        }
        
        // Find or create user
        User user = userService.createOrUpdateExternalUser(providerId, externalUserId, email, displayName);
        
        return generateAuthenticationResponse(user);
    }
    
    private AuthenticationResponse generateAuthenticationResponse(User user) {
        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userService.updateUser(user);
        
        // Get user privileges
        Set<String> privileges = userService.getUserPrivileges(user.getId());
        
        // Generate tokens
        String accessToken = jwtService.generateToken(user, privileges);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        // Save refresh token
        saveRefreshToken(user.getId(), refreshToken);
        
        // Create session info
        SessionInfo sessionInfo = jwtService.createSessionInfo(accessToken, user.getUsername(), privileges);
        SessionStore.setLocal(sessionInfo);
        
        return new AuthenticationResponse(accessToken, refreshToken);
    }
    
    private void saveRefreshToken(Long userId, String token) {
        RefreshToken refreshToken = new RefreshToken()
                .setToken(token)
                .setUserId(userId)
                .setExpiresAt(LocalDateTime.now().plusDays(7)) // Match the JWT refresh expiration
                .setRevoked(false)
                .setSysStatus("A")
                .setSysCreatedAt(LocalDateTime.now())
                .setSysCreatedBy("system")
                .setSysModifiedAt(LocalDateTime.now())
                .setSysModifiedBy("system")
                .setSysVersion(1);
        
        refreshTokenRepository.save(refreshToken);
    }
    
    public record AuthenticationRequest(String username, String password) {}
    
    public record AuthenticationResponse(String accessToken, String refreshToken) {}
}
