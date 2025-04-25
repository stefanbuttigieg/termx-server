package com.kodality.termx.uam.user.repository;

import com.kodality.termx.uam.user.model.UserPrivilege;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Set;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface UserPrivilegeRepository extends CrudRepository<UserPrivilege, Long> {
    
    List<UserPrivilege> findByUserId(Long userId);
    
    @Query("SELECT p.code FROM uam.user_privilege up " +
           "JOIN uam.privilege p ON up.privilege_id = p.id " +
           "WHERE up.user_id = :userId AND up.sys_status = 'A' AND p.sys_status = 'A'")
    Set<String> findPrivilegeCodesByUserId(Long userId);
    
    void deleteByUserId(Long userId);
    
    void deleteByUserIdAndPrivilegeId(Long userId, Long privilegeId);
}
