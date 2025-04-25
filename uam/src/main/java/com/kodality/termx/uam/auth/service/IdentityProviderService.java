package com.kodality.termx.uam.auth.service;

import com.kodality.commons.exception.BadRequestException;
import com.kodality.commons.exception.NotFoundException;
import com.kodality.termx.uam.auth.model.AuthProvider;
import com.kodality.termx.uam.auth.model.ProviderType;
import com.kodality.termx.uam.auth.provider.IdentityProviderAdapter;
import com.kodality.termx.uam.auth.repository.AuthProviderRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Singleton
public class IdentityProviderService {
    
    private final AuthProviderRepository providerRepository;
    private final Map<String, IdentityProviderAdapter> adaptersByType;
    private final Map<String, IdentityProviderAdapter> adaptersByProviderId;
    private final AuthenticationService authenticationService;
    
    public IdentityProviderService(
            AuthProviderRepository providerRepository,
            List<IdentityProviderAdapter> adapters,
            AuthenticationService authenticationService) {
        this.providerRepository = providerRepository;
        this.authenticationService = authenticationService;
        
        this.adaptersByType = new HashMap<>();
        this.adaptersByProviderId = new HashMap<>();
        
        for (IdentityProviderAdapter adapter : adapters) {
            adaptersByType.put(adapter.getProviderType(), adapter);
        }
        
        // Initialize adapters for existing providers
        providerRepository.findByEnabled(true).forEach(this::initializeAdapter);
    }
    
    @Transactional(readOnly = true)
    public List<AuthProvider> getAllProviders() {
        return (List<AuthProvider>) providerRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<AuthProvider> getEnabledProviders() {
        return providerRepository.findByEnabled(true);
    }
    
    @Transactional(readOnly = true)
    public AuthProvider getProviderById(Long id) {
        return providerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Provider not found with id: " + id));
    }
    
    @Transactional(readOnly = true)
    public AuthProvider getProviderByProviderId(String providerId) {
        return providerRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException("Provider not found with providerId: " + providerId));
    }
    
    @Transactional
    public AuthProvider createProvider(AuthProvider provider) {
        if (providerRepository.existsByProviderId(provider.getProviderId())) {
            throw new BadRequestException("Provider already exists with id: " + provider.getProviderId());
        }
        
        validateProvider(provider);
        
        provider.setSysStatus("A");
        provider.setSysCreatedAt(LocalDateTime.now());
        provider.setSysCreatedBy("system");
        provider.setSysModifiedAt(LocalDateTime.now());
        provider.setSysModifiedBy("system");
        provider.setSysVersion(1);
        
        AuthProvider savedProvider = providerRepository.save(provider);
        initializeAdapter(savedProvider);
        
        return savedProvider;
    }
    
    @Transactional
    public AuthProvider updateProvider(AuthProvider provider) {
        AuthProvider existingProvider = getProviderById(provider.getId());
        
        validateProvider(provider);
        
        provider.setSysStatus("A");
        provider.setSysModifiedAt(LocalDateTime.now());
        provider.setSysModifiedBy("system");
        provider.setSysVersion(existingProvider.getSysVersion() + 1);
        
        AuthProvider updatedProvider = providerRepository.update(provider);
        
        // Re-initialize adapter with updated config
        initializeAdapter(updatedProvider);
        
        return updatedProvider;
    }
    
    @Transactional
    public void deleteProvider(Long id) {
        AuthProvider provider = getProviderById(id);
        
        // Remove from adapters map
        adaptersByProviderId.remove(provider.getProviderId());
        
        // Soft delete
        provider.setSysStatus("D");
        provider.setEnabled(false);
        provider.setSysModifiedAt(LocalDateTime.now());
        provider.setSysModifiedBy("system");
        provider.setSysVersion(provider.getSysVersion() + 1);
        
        providerRepository.update(provider);
    }
    
    @Transactional
    public void enableProvider(Long id, boolean enabled) {
        AuthProvider provider = getProviderById(id);
        provider.setEnabled(enabled);
        provider.setSysModifiedAt(LocalDateTime.now());
        provider.setSysModifiedBy("system");
        provider.setSysVersion(provider.getSysVersion() + 1);
        
        AuthProvider updatedProvider = providerRepository.update(provider);
        
        if (enabled) {
            initializeAdapter(updatedProvider);
        } else {
            adaptersByProviderId.remove(provider.getProviderId());
        }
    }
    
    public String generateAuthorizationUrl(String providerId, String redirectUri) {
        IdentityProviderAdapter adapter = getAdapter(providerId);
        String state = generateState();
        return adapter.generateAuthorizationUrl(redirectUri, state);
    }
    
    public AuthenticationService.AuthenticationResponse processCallback(
            String providerId, String code, String redirectUri) {
        IdentityProviderAdapter adapter = getAdapter(providerId);
        IdentityProviderAdapter.ExternalUserInfo userInfo = adapter.processCallback(code, redirectUri);
        
        return authenticationService.authenticateWithExternalProvider(
                providerId,
                userInfo.id(),
                userInfo.email(),
                userInfo.displayName()
        );
    }
    
    private IdentityProviderAdapter getAdapter(String providerId) {
        IdentityProviderAdapter adapter = adaptersByProviderId.get(providerId);
        if (adapter == null) {
            AuthProvider provider = getProviderByProviderId(providerId);
            if (!provider.isEnabled()) {
                throw new BadRequestException("Identity provider is disabled: " + providerId);
            }
            
            initializeAdapter(provider);
            adapter = adaptersByProviderId.get(providerId);
        }
        
        if (adapter == null) {
            throw new BadRequestException("No adapter available for provider: " + providerId);
        }
        
        return adapter;
    }
    
    private void initializeAdapter(AuthProvider provider) {
        if (!provider.isEnabled()) {
            return;
        }
        
        IdentityProviderAdapter adapter = adaptersByType.get(provider.getType());
        if (adapter == null) {
            log.warn("No adapter found for provider type: {}", provider.getType());
            return;
        }
        
        try {
            adapter.initialize(provider);
            adaptersByProviderId.put(provider.getProviderId(), adapter);
        } catch (Exception e) {
            log.error("Failed to initialize adapter for provider: {}", provider.getProviderId(), e);
        }
    }
    
    private void validateProvider(AuthProvider provider) {
        ProviderType type;
        try {
            type = ProviderType.valueOf(provider.getType());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid provider type: " + provider.getType());
        }
        
        IdentityProviderAdapter adapter = adaptersByType.get(type.name());
        if (adapter == null) {
            throw new BadRequestException("No adapter available for provider type: " + type);
        }
        
        if (!adapter.validateConfiguration(provider)) {
            throw new BadRequestException("Invalid configuration for provider type: " + type);
        }
    }
    
    private String generateState() {
        return UUID.randomUUID().toString();
    }
}
