--liquibase formatted sql

--changeset uam:user_role
drop table if exists uam.user_role;
create table uam.user_role (
    id                  bigint default nextval('core.s_entity') primary key,
    user_id             bigint not null,
    role                text not null,

    sys_status          char(1) default 'A' not null,
    sys_created_at      timestamp not null,
    sys_created_by      text not null,
    sys_modified_at     timestamp not null,
    sys_modified_by     text not null,
    sys_version         int not null,
    
    constraint user_role_user_fk foreign key (user_id) references uam.user(id),
    constraint user_role_unique unique (user_id, role)
);

create index user_role_user_idx on uam.user_role(user_id);

select core.create_table_metadata('uam.user_role');
--rollback drop table if exists uam.user_role;
