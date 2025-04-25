package com.kodality.termx.uam.user.repository;

import com.kodality.termx.uam.user.model.User;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface UserRepository extends CrudRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    @Query("SELECT u.* FROM uam.user u JOIN uam.external_identity ei ON u.id = ei.user_id " +
           "WHERE ei.provider_id = :providerId AND ei.external_user_id = :externalUserId")
    Optional<User> findByExternalIdentity(String providerId, String externalUserId);
    
    List<User> findByStatus(String status);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
}
