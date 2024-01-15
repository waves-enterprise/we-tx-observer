drop index if exists ix__tx_queue_partition__priority_for_paused;

create index if not exists ix__tx_queue_partition__priority_id_for_paused
    on tx_queue_partition(priority desc, id)
    where paused_on_tx_id is null;