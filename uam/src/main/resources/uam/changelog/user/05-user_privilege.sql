--liquibase formatted sql

--changeset uam:user_privilege
drop table if exists uam.user_privilege;
create table uam.user_privilege (
    id                  bigint default nextval('core.s_entity') primary key,
    user_id             bigint not null,
    privilege_id        bigint not null,

    sys_status          char(1) default 'A' not null,
    sys_created_at      timestamp not null,
    sys_created_by      text not null,
    sys_modified_at     timestamp not null,
    sys_modified_by     text not null,
    sys_version         int not null,
    
    constraint user_privilege_user_fk foreign key (user_id) references uam.user(id),
    constraint user_privilege_privilege_fk foreign key (privilege_id) references uam.privilege(id),
    constraint user_privilege_unique unique (user_id, privilege_id)
);

create index user_privilege_user_idx on uam.user_privilege(user_id);
create index user_privilege_privilege_idx on uam.user_privilege(privilege_id);

select core.create_table_metadata('uam.user_privilege');
--rollback drop table if exists uam.user_privilege;
