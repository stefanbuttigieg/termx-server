package com.kodality.termx.uam.user.repository;

import com.kodality.termx.uam.user.model.UserRole;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Set;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface UserRoleRepository extends CrudRepository<UserRole, Long> {
    
    List<UserRole> findByUserId(Long userId);
    
    @Query("SELECT role FROM uam.user_role WHERE user_id = :userId AND sys_status = 'A'")
    Set<String> findRolesByUserId(Long userId);
    
    void deleteByUserId(Long userId);
    
    void deleteByUserIdAndRole(Long userId, String role);
}
