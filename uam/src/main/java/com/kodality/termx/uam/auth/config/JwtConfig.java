package com.kodality.termx.uam.auth.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Context;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Context
@ConfigurationProperties("jwt")
public class JwtConfig {
    private String secret;
    private long expiration = 86400000; // 24 hours in milliseconds
    private long refreshExpiration = 604800000; // 7 days in milliseconds
}
