--liquibase formatted sql

--changeset uam:default_admin
-- Create default admin user if no users exist
INSERT INTO uam.user (
    username, email, password_hash, first_name, last_name, status, email_verified,
    sys_status, sys_created_at, sys_created_by, sys_modified_at, sys_modified_by, sys_version
)
SELECT 
    'admin', 'admin@example.com', 
    -- Default password: admin123 (should be changed immediately in production)
    '$2a$12$8vxYfAWyUCpEkZQRZIXQo.LKCCt1u9JUGJm6gQfKBQ9i8TQJsm2Vy',
    'System', 'Administrator', 'ACTIVE', true,
    'A', now(), 'system', now(), 'system', 1
WHERE NOT EXISTS (SELECT 1 FROM uam.user LIMIT 1);

-- Create admin role for the admin user
INSERT INTO uam.user_role (
    user_id, role,
    sys_status, sys_created_at, sys_created_by, sys_modified_at, sys_modified_by, sys_version
)
SELECT 
    u.id, 'ADMIN',
    'A', now(), 'system', now(), 'system', 1
FROM uam.user u
WHERE u.username = 'admin'
AND NOT EXISTS (
    SELECT 1 FROM uam.user_role ur 
    WHERE ur.user_id = u.id AND ur.role = 'ADMIN'
);

-- Add admin privilege to admin user (wildcard privilege)
INSERT INTO uam.privilege (
    code, names,
    sys_status, sys_created_at, sys_created_by, sys_modified_at, sys_modified_by, sys_version
)
SELECT 
    '*.*.*', '{"en": "All Privileges"}',
    'A', now(), 'system', now(), 'system', 1
WHERE NOT EXISTS (
    SELECT 1 FROM uam.privilege p 
    WHERE p.code = '*.*.*'
);

INSERT INTO uam.user_privilege (
    user_id, privilege_id,
    sys_status, sys_created_at, sys_created_by, sys_modified_at, sys_modified_by, sys_version
)
SELECT 
    u.id, p.id,
    'A', now(), 'system', now(), 'system', 1
FROM uam.user u, uam.privilege p
WHERE u.username = 'admin' AND p.code = '*.*.*'
AND NOT EXISTS (
    SELECT 1 FROM uam.user_privilege up 
    WHERE up.user_id = u.id AND up.privilege_id = p.id
);
--rollback select 1 from dual;
