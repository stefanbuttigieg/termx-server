package com.kodality.termx.uam.auth.repository;

import com.kodality.termx.uam.auth.model.AuthProvider;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface AuthProviderRepository extends CrudRepository<AuthProvider, Long> {
    
    Optional<AuthProvider> findByProviderId(String providerId);
    
    List<AuthProvider> findByEnabled(boolean enabled);
    
    boolean existsByProviderId(String providerId);
}
