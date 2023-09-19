create table block_history
(
    id                uuid                     not null primary key,
    signature         varchar(1024)            not null,
    height            bigint                   not null,
    timestamp         timestamp with time zone not null,
    created_timestamp timestamp with time zone not null,
    deleted           boolean                  not null
);

create unique index if not exists ix__block_history__signature
    on block_history (signature)
    where deleted = false;

create index if not exists ix__block_history__height
    on block_history (height)
    where deleted = false;
