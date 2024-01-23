drop index if exists ix__tx_queue_partition__priority_for_paused;

create index if not exists ix__tx_queue_partition__priority
    on tx_queue_partition(priority desc);