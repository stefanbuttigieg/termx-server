package com.kodality.termx.uam.user.repository;

import com.kodality.termx.uam.user.model.ExternalIdentity;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface ExternalIdentityRepository extends CrudRepository<ExternalIdentity, Long> {
    
    List<ExternalIdentity> findByUserId(Long userId);
    
    Optional<ExternalIdentity> findByProviderIdAndExternalUserId(String providerId, String externalUserId);
    
    void deleteByUserId(Long userId);
}
