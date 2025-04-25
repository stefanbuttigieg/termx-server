package com.kodality.termx.uam.user.service;

import com.kodality.commons.exception.BadRequestException;
import com.kodality.commons.exception.NotFoundException;
import com.kodality.termx.uam.user.model.ExternalIdentity;
import com.kodality.termx.uam.user.model.User;
import com.kodality.termx.uam.user.model.UserPrivilege;
import com.kodality.termx.uam.user.model.UserRole;
import com.kodality.termx.uam.user.model.UserStatus;
import com.kodality.termx.uam.user.repository.ExternalIdentityRepository;
import com.kodality.termx.uam.user.repository.UserPrivilegeRepository;
import com.kodality.termx.uam.user.repository.UserRepository;
import com.kodality.termx.uam.user.repository.UserRoleRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Singleton
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final ExternalIdentityRepository externalIdentityRepository;
    private final UserPrivilegeRepository userPrivilegeRepository;
    private final UserRoleRepository userRoleRepository;
    
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }
    
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Transactional(readOnly = true)
    public Optional<User> findByExternalIdentity(String providerId, String externalUserId) {
        return userRepository.findByExternalIdentity(providerId, externalUserId);
    }
    
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return (List<User>) userRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<User> findByStatus(UserStatus status) {
        return userRepository.findByStatus(status.name());
    }
    
    @Transactional
    public User createUser(User user, String password) {
        validateNewUser(user);
        
        if (password != null && !password.isEmpty()) {
            user.setPasswordHash(hashPassword(password));
        }
        
        user.setStatus(UserStatus.ACTIVE);
        user.setSysStatus("A");
        user.setSysCreatedAt(LocalDateTime.now());
        user.setSysCreatedBy("system");
        user.setSysModifiedAt(LocalDateTime.now());
        user.setSysModifiedBy("system");
        user.setSysVersion(1);
        
        return userRepository.save(user);
    }
    
    @Transactional
    public User updateUser(User user) {
        User existingUser = findById(user.getId());
        
        // Don't update password hash through this method
        user.setPasswordHash(existingUser.getPasswordHash());
        
        user.setSysStatus("A");
        user.setSysModifiedAt(LocalDateTime.now());
        user.setSysModifiedBy("system");
        user.setSysVersion(existingUser.getSysVersion() + 1);
        
        return userRepository.update(user);
    }
    
    @Transactional
    public void changePassword(Long userId, String newPassword) {
        User user = findById(userId);
        user.setPasswordHash(hashPassword(newPassword));
        user.setSysModifiedAt(LocalDateTime.now());
        user.setSysModifiedBy("system");
        user.setSysVersion(user.getSysVersion() + 1);
        
        userRepository.update(user);
    }
    
    @Transactional
    public void deleteUser(Long id) {
        User user = findById(id);
        
        // Delete related entities
        externalIdentityRepository.deleteByUserId(id);
        userPrivilegeRepository.deleteByUserId(id);
        userRoleRepository.deleteByUserId(id);
        
        // Soft delete the user
        user.setSysStatus("D");
        user.setSysModifiedAt(LocalDateTime.now());
        user.setSysModifiedBy("system");
        user.setSysVersion(user.getSysVersion() + 1);
        
        userRepository.update(user);
    }
    
    @Transactional
    public ExternalIdentity addExternalIdentity(Long userId, ExternalIdentity externalIdentity) {
        User user = findById(userId);
        
        externalIdentity.setUserId(userId);
        externalIdentity.setSysStatus("A");
        externalIdentity.setSysCreatedAt(LocalDateTime.now());
        externalIdentity.setSysCreatedBy("system");
        externalIdentity.setSysModifiedAt(LocalDateTime.now());
        externalIdentity.setSysModifiedBy("system");
        externalIdentity.setSysVersion(1);
        
        return externalIdentityRepository.save(externalIdentity);
    }
    
    @Transactional
    public void removeExternalIdentity(Long userId, Long externalIdentityId) {
        ExternalIdentity identity = externalIdentityRepository.findById(externalIdentityId)
                .orElseThrow(() -> new NotFoundException("External identity not found"));
        
        if (!identity.getUserId().equals(userId)) {
            throw new BadRequestException("External identity does not belong to the specified user");
        }
        
        externalIdentityRepository.delete(identity);
    }
    
    @Transactional(readOnly = true)
    public List<ExternalIdentity> getUserExternalIdentities(Long userId) {
        return externalIdentityRepository.findByUserId(userId);
    }
    
    @Transactional
    public void addUserPrivilege(Long userId, Long privilegeId) {
        // Check if user exists
        findById(userId);
        
        UserPrivilege userPrivilege = new UserPrivilege()
                .setUserId(userId)
                .setPrivilegeId(privilegeId)
                .setSysStatus("A")
                .setSysCreatedAt(LocalDateTime.now())
                .setSysCreatedBy("system")
                .setSysModifiedAt(LocalDateTime.now())
                .setSysModifiedBy("system")
                .setSysVersion(1);
        
        userPrivilegeRepository.save(userPrivilege);
    }
    
    @Transactional
    public void removeUserPrivilege(Long userId, Long privilegeId) {
        userPrivilegeRepository.deleteByUserIdAndPrivilegeId(userId, privilegeId);
    }
    
    @Transactional(readOnly = true)
    public Set<String> getUserPrivileges(Long userId) {
        return userPrivilegeRepository.findPrivilegeCodesByUserId(userId);
    }
    
    @Transactional
    public void addUserRole(Long userId, String role) {
        // Check if user exists
        findById(userId);
        
        UserRole userRole = new UserRole()
                .setUserId(userId)
                .setRole(role)
                .setSysStatus("A")
                .setSysCreatedAt(LocalDateTime.now())
                .setSysCreatedBy("system")
                .setSysModifiedAt(LocalDateTime.now())
                .setSysModifiedBy("system")
                .setSysVersion(1);
        
        userRoleRepository.save(userRole);
    }
    
    @Transactional
    public void removeUserRole(Long userId, String role) {
        userRoleRepository.deleteByUserIdAndRole(userId, role);
    }
    
    @Transactional(readOnly = true)
    public Set<String> getUserRoles(Long userId) {
        return userRoleRepository.findRolesByUserId(userId);
    }
    
    @Transactional
    public User createOrUpdateExternalUser(String providerId, String externalUserId, String email, String displayName) {
        Optional<User> existingUser = userRepository.findByExternalIdentity(providerId, externalUserId);
        
        if (existingUser.isPresent()) {
            // Update existing external identity
            ExternalIdentity identity = externalIdentityRepository.findByProviderIdAndExternalUserId(providerId, externalUserId)
                    .orElseThrow(() -> new IllegalStateException("External identity not found for existing user"));
            
            identity.setEmail(email);
            identity.setDisplayName(displayName);
            identity.setLastLogin(LocalDateTime.now());
            identity.setSysModifiedAt(LocalDateTime.now());
            identity.setSysModifiedBy("system");
            identity.setSysVersion(identity.getSysVersion() + 1);
            
            externalIdentityRepository.update(identity);
            
            // Update user's last login
            User user = existingUser.get();
            user.setLastLogin(LocalDateTime.now());
            user.setSysModifiedAt(LocalDateTime.now());
            user.setSysModifiedBy("system");
            user.setSysVersion(user.getSysVersion() + 1);
            
            return userRepository.update(user);
        } else {
            // Create new user with external identity
            User newUser = new User()
                    .setUsername(generateUsername(email, displayName))
                    .setEmail(email)
                    .setFirstName(extractFirstName(displayName))
                    .setLastName(extractLastName(displayName))
                    .setStatus(UserStatus.ACTIVE)
                    .setLastLogin(LocalDateTime.now())
                    .setEmailVerified(true) // Assume email is verified through external provider
                    .setExternalIdentities(new HashSet<>());
            
            User savedUser = createUser(newUser, null);
            
            // Create external identity
            ExternalIdentity identity = new ExternalIdentity()
                    .setUserId(savedUser.getId())
                    .setProviderId(providerId)
                    .setExternalUserId(externalUserId)
                    .setEmail(email)
                    .setDisplayName(displayName)
                    .setLastLogin(LocalDateTime.now());
            
            ExternalIdentity savedIdentity = addExternalIdentity(savedUser.getId(), identity);
            
            // Add default role
            addUserRole(savedUser.getId(), "USER");
            
            return savedUser;
        }
    }
    
    public boolean validatePassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
    
    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }
    
    private void validateNewUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new BadRequestException("Username already exists: " + user.getUsername());
        }
        
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BadRequestException("Email already exists: " + user.getEmail());
        }
    }
    
    private String generateUsername(String email, String displayName) {
        // Try to generate username from email first
        String username = email.split("@")[0];
        
        // Check if username exists
        if (!userRepository.existsByUsername(username)) {
            return username;
        }
        
        // Try to generate from display name
        if (displayName != null && !displayName.isEmpty()) {
            username = displayName.toLowerCase().replaceAll("\\s+", ".");
            
            if (!userRepository.existsByUsername(username)) {
                return username;
            }
        }
        
        // Add random suffix until we find a unique username
        int suffix = 1;
        while (userRepository.existsByUsername(username + suffix)) {
            suffix++;
        }
        
        return username + suffix;
    }
    
    private String extractFirstName(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return null;
        }
        
        String[] parts = displayName.split("\\s+");
        return parts[0];
    }
    
    private String extractLastName(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return null;
        }
        
        String[] parts = displayName.split("\\s+");
        if (parts.length > 1) {
            return parts[parts.length - 1];
        }
        
        return null;
    }
}
