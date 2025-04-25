package com.kodality.termx.uam.auth.repository;

import com.kodality.termx.uam.auth.model.RefreshToken;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    List<RefreshToken> findByUserId(Long userId);
    
    @Query("SELECT * FROM uam.refresh_token WHERE user_id = :userId AND revoked = false AND expires_at > :now")
    List<RefreshToken> findValidTokensByUserId(Long userId, LocalDateTime now);
    
    @Query("UPDATE uam.refresh_token SET revoked = true, sys_modified_at = :now, sys_modified_by = 'system', sys_version = sys_version + 1 " +
           "WHERE user_id = :userId AND revoked = false")
    void revokeAllUserTokens(Long userId, LocalDateTime now);
    
    @Query("DELETE FROM uam.refresh_token WHERE expires_at < :expiryDate")
    void deleteExpiredTokens(LocalDateTime expiryDate);
}
