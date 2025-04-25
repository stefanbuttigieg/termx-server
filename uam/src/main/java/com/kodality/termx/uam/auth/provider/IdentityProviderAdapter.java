package com.kodality.termx.uam.auth.provider;

import com.kodality.termx.uam.auth.model.AuthProvider;

/**
 * Interface for identity provider adapters.
 * Each external identity provider should implement this interface.
 */
public interface IdentityProviderAdapter {
    
    /**
     * Get the provider type this adapter supports.
     */
    String getProviderType();
    
    /**
     * Initialize the adapter with provider configuration.
     */
    void initialize(AuthProvider provider);
    
    /**
     * Generate the authorization URL for the user to be redirected to.
     */
    String generateAuthorizationUrl(String redirectUri, String state);
    
    /**
     * Process the callback from the identity provider and extract user information.
     */
    ExternalUserInfo processCallback(String code, String redirectUri);
    
    /**
     * Validate the provider configuration.
     */
    boolean validateConfiguration(AuthProvider provider);
    
    /**
     * Class to hold external user information.
     */
    record ExternalUserInfo(
            String id,
            String email,
            String displayName,
            String firstName,
            String lastName,
            String accessToken,
            String refreshToken,
            Long expiresIn
    ) {}
}
