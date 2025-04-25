--liquibase formatted sql

--changeset uam:user
drop table if exists uam.user;
create table uam.user (
    id                  bigint default nextval('core.s_entity') primary key,
    username            text not null unique,
    email               text not null unique,
    password_hash       text,
    first_name          text,
    last_name           text,
    status              text not null default 'ACTIVE',
    last_login          timestamp,
    email_verified      boolean not null default false,

    sys_status          char(1) default 'A' not null,
    sys_created_at      timestamp not null,
    sys_created_by      text not null,
    sys_modified_at     timestamp not null,
    sys_modified_by     text not null,
    sys_version         int not null
);

create index user_username_idx on uam.user(username);
create index user_email_idx on uam.user(email);

select core.create_table_metadata('uam.user');
--rollback drop table if exists uam.user;
