create table if not exists block_height_reset
(
    id              varchar not null
        constraint block_height_reset_pkey
            primary key,
    height_to_reset bigint  not null
)
