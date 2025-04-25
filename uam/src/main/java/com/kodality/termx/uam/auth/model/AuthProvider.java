package com.kodality.termx.uam.auth.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Accessors(chain = true)
public class AuthProvider {
    private Long id;
    private String providerId;
    private String name;
    private String description;
    private ProviderType type;
    private boolean enabled;
    private Map<String, String> config;
    
    // System metadata
    private String sysStatus;
    private LocalDateTime sysCreatedAt;
    private String sysCreatedBy;
    private LocalDateTime sysModifiedAt;
    private String sysModifiedBy;
    private Integer sysVersion;
}
