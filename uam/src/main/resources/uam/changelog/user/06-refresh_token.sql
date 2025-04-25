--liquibase formatted sql

--changeset uam:refresh_token
drop table if exists uam.refresh_token;
create table uam.refresh_token (
    id                  bigint default nextval('core.s_entity') primary key,
    token               text not null unique,
    user_id             bigint not null,
    expires_at          timestamp not null,
    revoked             boolean not null default false,
    
    sys_status          char(1) default 'A' not null,
    sys_created_at      timestamp not null,
    sys_created_by      text not null,
    sys_modified_at     timestamp not null,
    sys_modified_by     text not null,
    sys_version         int not null,
    
    constraint refresh_token_user_fk foreign key (user_id) references uam.user(id)
);

create index refresh_token_user_idx on uam.refresh_token(user_id);
create index refresh_token_token_idx on uam.refresh_token(token);
create index refresh_token_expires_idx on uam.refresh_token(expires_at);

select core.create_table_metadata('uam.refresh_token');
--rollback drop table if exists uam.refresh_token;
