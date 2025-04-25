package com.kodality.termx.uam.user.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class ExternalIdentity {
    private Long id;
    private Long userId;
    private String providerId;
    private String externalUserId;
    private String email;
    private String displayName;
    private LocalDateTime lastLogin;
    
    // System metadata
    private String sysStatus;
    private LocalDateTime sysCreatedAt;
    private String sysCreatedBy;
    private LocalDateTime sysModifiedAt;
    private String sysModifiedBy;
    private Integer sysVersion;
}
