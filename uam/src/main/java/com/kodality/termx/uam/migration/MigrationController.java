package com.kodality.termx.uam.migration;

import com.kodality.termx.core.auth.Authorized;
import com.kodality.termx.uam.user.model.User;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Controller("/admin/migration")
@RequiredArgsConstructor
public class MigrationController {
    
    private final KeycloakMigrationService keycloakMigrationService;
    
    @Authorized("admin.migration.execute")
    @Post("/keycloak")
    public HttpResponse<MigrationResult> migrateKeycloakUsers() {
        List<User> migratedUsers = keycloakMigrationService.migrateUsers();
        return HttpResponse.ok(new MigrationResult(migratedUsers.size(), "Successfully migrated users from Keycloak"));
    }
    
    public record MigrationResult(int count, String message) {}
}
