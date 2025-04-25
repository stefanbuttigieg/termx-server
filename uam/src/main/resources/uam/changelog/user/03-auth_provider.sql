--liquibase formatted sql

--changeset uam:auth_provider
drop table if exists uam.auth_provider;
create table uam.auth_provider (
    id                  bigint default nextval('core.s_entity') primary key,
    provider_id         text not null unique,
    name                text not null,
    description         text,
    type                text not null,
    enabled             boolean not null default true,
    config              jsonb,

    sys_status          char(1) default 'A' not null,
    sys_created_at      timestamp not null,
    sys_created_by      text not null,
    sys_modified_at     timestamp not null,
    sys_modified_by     text not null,
    sys_version         int not null
);

create index auth_provider_provider_id_idx on uam.auth_provider(provider_id);

select core.create_table_metadata('uam.auth_provider');
--rollback drop table if exists uam.auth_provider;
