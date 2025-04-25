--liquibase formatted sql

--changeset uam:external_identity
drop table if exists uam.external_identity;
create table uam.external_identity (
    id                  bigint default nextval('core.s_entity') primary key,
    user_id             bigint not null,
    provider_id         text not null,
    external_user_id    text not null,
    email               text,
    display_name        text,
    last_login          timestamp,

    sys_status          char(1) default 'A' not null,
    sys_created_at      timestamp not null,
    sys_created_by      text not null,
    sys_modified_at     timestamp not null,
    sys_modified_by     text not null,
    sys_version         int not null,
    
    constraint external_identity_user_fk foreign key (user_id) references uam.user(id),
    constraint external_identity_unique unique (provider_id, external_user_id)
);

create index external_identity_user_idx on uam.external_identity(user_id);
create index external_identity_provider_idx on uam.external_identity(provider_id);

select core.create_table_metadata('uam.external_identity');
--rollback drop table if exists uam.external_identity;
