package com.kodality.termx.uam.user.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.micronaut.core.annotation.Nullable;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Accessors(chain = true)
public class User {
    private Long id;
    private String username;
    private String email;
    
    @JsonIgnore
    private String passwordHash;
    
    private String firstName;
    private String lastName;
    private UserStatus status;
    private LocalDateTime lastLogin;
    private boolean emailVerified;
    
    @Nullable
    private Set<ExternalIdentity> externalIdentities = new HashSet<>();
    
    // System metadata
    private String sysStatus;
    private LocalDateTime sysCreatedAt;
    private String sysCreatedBy;
    private LocalDateTime sysModifiedAt;
    private String sysModifiedBy;
    private Integer sysVersion;
}
