--region BlockHeightInfo
create table if not exists block_height_info
(
    id                uuid         not null
        constraint block_height_info_pkey
            primary key,
    created_timestamp timestamp,
    current_height    bigint       not null,
    node_alias        varchar(255) not null,
    update_timestamp  timestamp,
    version           bigint       not null
);

create unique index if not exists block_height_info_one_row
    on block_height_info ((node_alias is not null));

create function block_height_info_no_delete()
    returns trigger
    language plpgsql as
$f$
begin
    raise exception 'You may not delete the block_height_info row!';
end
$f$;

create trigger block_height_info_no_delete
    before delete
    on block_height_info
    for each row
execute procedure block_height_info_no_delete();
--endregion

--region TxQueuePartition
create table if not exists tx_queue_partition
(
    id                         varchar(255) not null
        constraint tx_queue_partition_pkey
            primary key,
    priority                   integer      not null,
    last_enqueued_tx_timestamp timestamp,
    last_enqueued_tx_id        varchar(255) not null,
    last_read_tx_id            varchar(255),
    created                    timestamp,
    modified                   timestamp
);

create index if not exists ix__tx_queue_partition__priority_timestamp_partial
    on tx_queue_partition (priority desc, last_enqueued_tx_timestamp asc)
    where (((last_read_tx_id)::text <> (last_enqueued_tx_id)::text) OR (last_read_tx_id IS NULL));
--endregion

--region EnqueuedTx
create table if not exists enqueued_tx
(
    id                varchar(255) not null
        constraint enqueued_tx_pkey
            primary key,
    status            varchar(255) not null,
    body              jsonb        not null,
    block_height      bigint       not null,
    position_in_block integer      not null,
    tx_timestamp      timestamp,
    created           timestamp,
    modified          timestamp,
    partition_id      varchar(255) not null
        constraint fk_partition_id
            references tx_queue_partition
);

create index if not exists enqueued_tx_created_idx
    on enqueued_tx (created);

create index if not exists ix__enqueued_tx__block_height_position_partition_status
    on enqueued_tx (block_height, position_in_block, partition_id, status);

create index if not exists ix_enqueued_tx_block_height_status
    on enqueued_tx (block_height)
    where ((status)::text = 'NEW'::text);
--endregion
