create table rollback_info
(
    id                 uuid                     not null primary key,
    to_height          bigint                   not null,
    to_block_signature varchar(1024)            not null,
    created_timestamp  timestamp with time zone not null
);

create index if not exists ix__rollback_info__created_timestamp
    on rollback_info (created_timestamp);

create index if not exists ix__rollback_info__to_height
    on rollback_info (to_height);
