package com.kodality.termx.uam.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodality.commons.exception.BadRequestException;
import com.kodality.termx.uam.user.model.ExternalIdentity;
import com.kodality.termx.uam.user.model.User;
import com.kodality.termx.uam.user.model.UserStatus;
import com.kodality.termx.uam.user.service.UserService;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient.Builder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class KeycloakMigrationService {
    
    private final UserService userService;
    private final ObjectMapper objectMapper;
    
    @Value("${keycloak.url:}")
    private String keycloakUrl;
    
    @Value("${keycloak.realm:termx}")
    private String realm;
    
    @Value("${keycloak.client-id:admin-cli}")
    private String clientId;
    
    @Value("${keycloak.client-secret:}")
    private String clientSecret;
    
    @Value("${keycloak.admin-username:}")
    private String adminUsername;
    
    @Value("${keycloak.admin-password:}")
    private String adminPassword;
    
    @Transactional
    public List<User> migrateUsers() {
        if (keycloakUrl == null || keycloakUrl.isEmpty()) {
            throw new BadRequestException("Keycloak URL is not configured");
        }
        
        try {
            // Get admin token
            String adminToken = getAdminToken();
            
            // Get users from Keycloak
            List<JsonNode> keycloakUsers = getKeycloakUsers(adminToken);
            
            List<User> migratedUsers = new ArrayList<>();
            
            // Process each user
            for (JsonNode keycloakUser : keycloakUsers) {
                try {
                    User user = processKeycloakUser(keycloakUser);
                    if (user != null) {
                        migratedUsers.add(user);
                    }
                } catch (Exception e) {
                    log.error("Error processing Keycloak user: {}", keycloakUser.get("username").asText(), e);
                }
            }
            
            return migratedUsers;
        } catch (Exception e) {
            log.error("Error migrating users from Keycloak", e);
            throw new BadRequestException("Failed to migrate users from Keycloak: " + e.getMessage());
        }
    }
    
    private String getAdminToken() throws IOException, InterruptedException {
        String tokenUrl = keycloakUrl + "/realms/master/protocol/openid-connect/token";
        
        Map<String, String> formData = new HashMap<>();
        formData.put("grant_type", "password");
        formData.put("client_id", clientId);
        
        if (clientSecret != null && !clientSecret.isEmpty()) {
            formData.put("client_secret", clientSecret);
        }
        
        formData.put("username", adminUsername);
        formData.put("password", adminPassword);
        
        String requestBody = formData.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder().build();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP error " + response.statusCode() + ": " + response.body());
        }
        
        JsonNode tokenResponse = objectMapper.readTree(response.body());
        return tokenResponse.get("access_token").asText();
    }
    
    private List<JsonNode> getKeycloakUsers(String adminToken) throws IOException, InterruptedException {
        String usersUrl = keycloakUrl + "/admin/realms/" + realm + "/users";
        
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder().build();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create(usersUrl))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build();
        
        java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP error " + response.statusCode() + ": " + response.body());
        }
        
        return objectMapper.readTree(response.body()).findValues("id").stream()
                .map(id -> getUserDetails(id.asText(), adminToken))
                .collect(Collectors.toList());
    }
    
    private JsonNode getUserDetails(String userId, String adminToken) {
        try {
            String userUrl = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId;
            
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder().build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(userUrl))
                    .header("Authorization", "Bearer " + adminToken)
                    .GET()
                    .build();
            
            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 400) {
                throw new IOException("HTTP error " + response.statusCode() + ": " + response.body());
            }
            
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            log.error("Error getting user details for user ID: {}", userId, e);
            return null;
        }
    }
    
    private User processKeycloakUser(JsonNode keycloakUser) {
        String username = keycloakUser.get("username").asText();
        String email = keycloakUser.has("email") ? keycloakUser.get("email").asText() : username + "@example.com";
        String firstName = keycloakUser.has("firstName") ? keycloakUser.get("firstName").asText() : null;
        String lastName = keycloakUser.has("lastName") ? keycloakUser.get("lastName").asText() : null;
        boolean enabled = keycloakUser.has("enabled") && keycloakUser.get("enabled").asBoolean();
        boolean emailVerified = keycloakUser.has("emailVerified") && keycloakUser.get("emailVerified").asBoolean();
        
        // Check if user already exists
        if (userService.findByUsername(username).isPresent()) {
            log.info("User already exists: {}", username);
            return null;
        }
        
        // Create user
        User user = new User()
                .setUsername(username)
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setStatus(enabled ? UserStatus.ACTIVE : UserStatus.INACTIVE)
                .setEmailVerified(emailVerified);
        
        User savedUser = userService.createUser(user, null); // No password, will use external identity
        
        // Add external identity for Keycloak
        ExternalIdentity identity = new ExternalIdentity()
                .setProviderId("keycloak")
                .setExternalUserId(keycloakUser.get("id").asText())
                .setEmail(email)
                .setDisplayName(firstName + " " + lastName);
        
        userService.addExternalIdentity(savedUser.getId(), identity);
        
        // Add default role
        userService.addUserRole(savedUser.getId(), "USER");
        
        // Process federated identities if available
        if (keycloakUser.has("federatedIdentities") && keycloakUser.get("federatedIdentities").isArray()) {
            for (JsonNode fedIdentity : keycloakUser.get("federatedIdentities")) {
                String identityProvider = fedIdentity.get("identityProvider").asText();
                String userId = fedIdentity.get("userId").asText();
                
                ExternalIdentity extIdentity = new ExternalIdentity()
                        .setProviderId(identityProvider)
                        .setExternalUserId(userId)
                        .setEmail(email)
                        .setDisplayName(firstName + " " + lastName);
                
                userService.addExternalIdentity(savedUser.getId(), extIdentity);
            }
        }
        
        return savedUser;
    }
}
