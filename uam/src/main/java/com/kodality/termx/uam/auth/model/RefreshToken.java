package com.kodality.termx.uam.auth.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class RefreshToken {
    private Long id;
    private String token;
    private Long userId;
    private LocalDateTime expiresAt;
    private boolean revoked;
    
    // System metadata
    private String sysStatus;
    private LocalDateTime sysCreatedAt;
    private String sysCreatedBy;
    private LocalDateTime sysModifiedAt;
    private String sysModifiedBy;
    private Integer sysVersion;
}
