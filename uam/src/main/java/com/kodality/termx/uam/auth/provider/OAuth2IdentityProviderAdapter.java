package com.kodality.termx.uam.auth.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodality.commons.exception.BadRequestException;
import com.kodality.termx.uam.auth.model.AuthProvider;
import com.kodality.termx.uam.auth.model.ProviderType;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class OAuth2IdentityProviderAdapter implements IdentityProviderAdapter {
    
    private static final String PROVIDER_TYPE = ProviderType.OAUTH2.name();
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    private AuthProvider provider;
    private String clientId;
    private String clientSecret;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String userInfoEndpoint;
    private String scope;
    private Map<String, String> userInfoMapping;
    
    public OAuth2IdentityProviderAdapter() {
        this.httpClient = HttpClient.newBuilder().build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getProviderType() {
        return PROVIDER_TYPE;
    }
    
    @Override
    public void initialize(AuthProvider provider) {
        this.provider = provider;
        
        Map<String, String> config = provider.getConfig();
        this.clientId = config.get("clientId");
        this.clientSecret = config.get("clientSecret");
        this.authorizationEndpoint = config.get("authorizationEndpoint");
        this.tokenEndpoint = config.get("tokenEndpoint");
        this.userInfoEndpoint = config.get("userInfoEndpoint");
        this.scope = config.getOrDefault("scope", "");
        
        // User info field mapping
        this.userInfoMapping = new HashMap<>();
        this.userInfoMapping.put("id", config.getOrDefault("userIdField", "id"));
        this.userInfoMapping.put("email", config.getOrDefault("userEmailField", "email"));
        this.userInfoMapping.put("name", config.getOrDefault("userNameField", "name"));
        this.userInfoMapping.put("firstName", config.getOrDefault("userFirstNameField", "first_name"));
        this.userInfoMapping.put("lastName", config.getOrDefault("userLastNameField", "last_name"));
        
        if (clientId == null || authorizationEndpoint == null || tokenEndpoint == null) {
            throw new IllegalArgumentException("Missing required configuration for OAuth2 provider");
        }
    }
    
    @Override
    public String generateAuthorizationUrl(String redirectUri, String state) {
        return authorizationEndpoint + "?" +
                "client_id=" + clientId + "&" +
                "response_type=code&" +
                "scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) + "&" +
                "redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) + "&" +
                "state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
    }
    
    @Override
    public ExternalUserInfo processCallback(String code, String redirectUri) {
        try {
            // Exchange code for tokens
            Map<String, String> tokenRequest = new HashMap<>();
            tokenRequest.put("client_id", clientId);
            if (clientSecret != null) {
                tokenRequest.put("client_secret", clientSecret);
            }
            tokenRequest.put("grant_type", "authorization_code");
            tokenRequest.put("code", code);
            tokenRequest.put("redirect_uri", redirectUri);
            
            String tokenResponse = executePost(tokenEndpoint, tokenRequest);
            JsonNode tokenJson = objectMapper.readTree(tokenResponse);
            
            String accessToken = tokenJson.get("access_token").asText();
            String refreshToken = tokenJson.has("refresh_token") ? tokenJson.get("refresh_token").asText() : null;
            Long expiresIn = tokenJson.has("expires_in") ? tokenJson.get("expires_in").asLong() : 3600L;
            
            // Get user info
            String userInfoResponse = executeGet(userInfoEndpoint, accessToken);
            JsonNode userInfo = objectMapper.readTree(userInfoResponse);
            
            String id = extractField(userInfo, userInfoMapping.get("id"));
            String email = extractField(userInfo, userInfoMapping.get("email"));
            String name = extractField(userInfo, userInfoMapping.get("name"));
            String firstName = extractField(userInfo, userInfoMapping.get("firstName"));
            String lastName = extractField(userInfo, userInfoMapping.get("lastName"));
            
            if (id == null) {
                throw new BadRequestException("User ID not found in OAuth2 response");
            }
            
            return new ExternalUserInfo(
                    id,
                    email,
                    name,
                    firstName,
                    lastName,
                    accessToken,
                    refreshToken,
                    expiresIn
            );
        } catch (Exception e) {
            log.error("Error processing OAuth2 callback", e);
            throw new BadRequestException("Failed to process OAuth2 callback: " + e.getMessage());
        }
    }
    
    @Override
    public boolean validateConfiguration(AuthProvider provider) {
        Map<String, String> config = provider.getConfig();
        return config != null &&
                config.containsKey("clientId") &&
                config.containsKey("authorizationEndpoint") &&
                config.containsKey("tokenEndpoint") &&
                config.containsKey("userInfoEndpoint");
    }
    
    private String extractField(JsonNode json, String fieldPath) {
        if (fieldPath == null || json == null) {
            return null;
        }
        
        String[] parts = fieldPath.split("\\.");
        JsonNode current = json;
        
        for (String part : parts) {
            if (current.has(part)) {
                current = current.get(part);
            } else {
                return null;
            }
        }
        
        return current.isTextual() ? current.asText() : current.toString();
    }
    
    private String executePost(String url, Map<String, String> formData) throws IOException, InterruptedException {
        String requestBody = formData.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP error " + response.statusCode() + ": " + response.body());
        }
        
        return response.body();
    }
    
    private String executeGet(String url, String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP error " + response.statusCode() + ": " + response.body());
        }
        
        return response.body();
    }
}
