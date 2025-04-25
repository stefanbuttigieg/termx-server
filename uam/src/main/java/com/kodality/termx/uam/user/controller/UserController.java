package com.kodality.termx.uam.user.controller;

import com.kodality.termx.core.auth.Authorized;
import com.kodality.termx.uam.user.model.ExternalIdentity;
import com.kodality.termx.uam.user.model.User;
import com.kodality.termx.uam.user.model.UserStatus;
import com.kodality.termx.uam.user.service.UserService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@Controller("/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @Authorized("admin.user.view")
    @Get
    public HttpResponse<List<User>> getAllUsers() {
        return HttpResponse.ok(userService.findAll());
    }
    
    @Authorized("admin.user.view")
    @Get("/{id}")
    public HttpResponse<User> getUserById(@PathVariable Long id) {
        return HttpResponse.ok(userService.findById(id));
    }
    
    @Authorized("admin.user.create")
    @Post
    public HttpResponse<User> createUser(@Body CreateUserRequest request) {
        User user = new User()
                .setUsername(request.username())
                .setEmail(request.email())
                .setFirstName(request.firstName())
                .setLastName(request.lastName())
                .setStatus(UserStatus.ACTIVE)
                .setEmailVerified(false);
        
        return HttpResponse.created(userService.createUser(user, request.password()));
    }
    
    @Authorized("admin.user.update")
    @Put("/{id}")
    public HttpResponse<User> updateUser(@PathVariable Long id, @Body UpdateUserRequest request) {
        User user = userService.findById(id);
        
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        
        return HttpResponse.ok(userService.updateUser(user));
    }
    
    @Authorized("admin.user.delete")
    @Delete("/{id}")
    public HttpResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return HttpResponse.noContent();
    }
    
    @Authorized("admin.user.update")
    @Post("/{id}/change-password")
    public HttpResponse<Void> changePassword(@PathVariable Long id, @Body ChangePasswordRequest request) {
        userService.changePassword(id, request.newPassword());
        return HttpResponse.ok();
    }
    
    @Authorized("admin.user.view")
    @Get("/{id}/external-identities")
    public HttpResponse<List<ExternalIdentity>> getUserExternalIdentities(@PathVariable Long id) {
        return HttpResponse.ok(userService.getUserExternalIdentities(id));
    }
    
    @Authorized("admin.user.update")
    @Delete("/{userId}/external-identities/{identityId}")
    public HttpResponse<Void> removeExternalIdentity(@PathVariable Long userId, @PathVariable Long identityId) {
        userService.removeExternalIdentity(userId, identityId);
        return HttpResponse.noContent();
    }
    
    @Authorized("admin.user.view")
    @Get("/{id}/privileges")
    public HttpResponse<Set<String>> getUserPrivileges(@PathVariable Long id) {
        return HttpResponse.ok(userService.getUserPrivileges(id));
    }
    
    @Authorized("admin.user.update")
    @Post("/{id}/privileges")
    public HttpResponse<Void> addUserPrivilege(@PathVariable Long id, @Body PrivilegeRequest request) {
        userService.addUserPrivilege(id, request.privilegeId());
        return HttpResponse.ok();
    }
    
    @Authorized("admin.user.update")
    @Delete("/{id}/privileges/{privilegeId}")
    public HttpResponse<Void> removeUserPrivilege(@PathVariable Long id, @PathVariable Long privilegeId) {
        userService.removeUserPrivilege(id, privilegeId);
        return HttpResponse.noContent();
    }
    
    @Authorized("admin.user.view")
    @Get("/{id}/roles")
    public HttpResponse<Set<String>> getUserRoles(@PathVariable Long id) {
        return HttpResponse.ok(userService.getUserRoles(id));
    }
    
    @Authorized("admin.user.update")
    @Post("/{id}/roles")
    public HttpResponse<Void> addUserRole(@PathVariable Long id, @Body RoleRequest request) {
        userService.addUserRole(id, request.role());
        return HttpResponse.ok();
    }
    
    @Authorized("admin.user.update")
    @Delete("/{id}/roles/{role}")
    public HttpResponse<Void> removeUserRole(@PathVariable Long id, @PathVariable String role) {
        userService.removeUserRole(id, role);
        return HttpResponse.noContent();
    }
    
    public record CreateUserRequest(
            String username,
            String email,
            String password,
            String firstName,
            String lastName
    ) {}
    
    public record UpdateUserRequest(
            String email,
            String firstName,
            String lastName,
            UserStatus status
    ) {}
    
    public record ChangePasswordRequest(String newPassword) {}
    
    public record PrivilegeRequest(Long privilegeId) {}
    
    public record RoleRequest(String role) {}
}
