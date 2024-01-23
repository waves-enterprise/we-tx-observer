drop index if exists ix__enqueued_tx_partition_id_timestamp_for_new;

create index if not exists ix__enqueued_tx_partition_id_for_new
    on enqueued_tx(partition_id)
    where (status = 'NEW');