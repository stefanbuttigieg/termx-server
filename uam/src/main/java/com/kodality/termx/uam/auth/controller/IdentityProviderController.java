package com.kodality.termx.uam.auth.controller;

import com.kodality.termx.core.auth.Authorized;
import com.kodality.termx.uam.auth.model.AuthProvider;
import com.kodality.termx.uam.auth.service.AuthenticationService.AuthenticationResponse;
import com.kodality.termx.uam.auth.service.IdentityProviderService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Controller("/auth/providers")
@RequiredArgsConstructor
public class IdentityProviderController {
    
    private final IdentityProviderService identityProviderService;
    
    @Get
    public HttpResponse<List<AuthProvider>> getAllProviders() {
        return HttpResponse.ok(identityProviderService.getAllProviders());
    }
    
    @Get("/enabled")
    public HttpResponse<List<AuthProvider>> getEnabledProviders() {
        return HttpResponse.ok(identityProviderService.getEnabledProviders());
    }
    
    @Get("/{id}")
    public HttpResponse<AuthProvider> getProviderById(@PathVariable Long id) {
        return HttpResponse.ok(identityProviderService.getProviderById(id));
    }
    
    @Authorized("admin.provider.create")
    @Post
    public HttpResponse<AuthProvider> createProvider(@Body AuthProvider provider) {
        return HttpResponse.created(identityProviderService.createProvider(provider));
    }
    
    @Authorized("admin.provider.update")
    @Put("/{id}")
    public HttpResponse<AuthProvider> updateProvider(@PathVariable Long id, @Body AuthProvider provider) {
        provider.setId(id);
        return HttpResponse.ok(identityProviderService.updateProvider(provider));
    }
    
    @Authorized("admin.provider.delete")
    @Delete("/{id}")
    public HttpResponse<Void> deleteProvider(@PathVariable Long id) {
        identityProviderService.deleteProvider(id);
        return HttpResponse.noContent();
    }
    
    @Authorized("admin.provider.update")
    @Put("/{id}/enable")
    public HttpResponse<Void> enableProvider(@PathVariable Long id, @Body EnableRequest request) {
        identityProviderService.enableProvider(id, request.enabled());
        return HttpResponse.ok();
    }
    
    @Get("/{providerId}/authorize")
    public HttpResponse<AuthorizationUrlResponse> getAuthorizationUrl(
            @PathVariable String providerId,
            @QueryValue String redirectUri) {
        String authUrl = identityProviderService.generateAuthorizationUrl(providerId, redirectUri);
        return HttpResponse.ok(new AuthorizationUrlResponse(authUrl));
    }
    
    @Post("/{providerId}/callback")
    public HttpResponse<AuthenticationResponse> handleCallback(
            @PathVariable String providerId,
            @Body CallbackRequest request) {
        AuthenticationResponse response = identityProviderService.processCallback(
                providerId, request.code(), request.redirectUri());
        return HttpResponse.ok(response);
    }
    
    public record EnableRequest(boolean enabled) {}
    
    public record AuthorizationUrlResponse(String authorizationUrl) {}
    
    public record CallbackRequest(String code, String redirectUri) {}
}
