drop index ix__enqueued_tx__block_height_position_partition_status;

create index if not exists ix__enqueued_tx__block_height_position_partition_status
    on enqueued_tx (block_height, position_in_block, position_in_atomic, partition_id, status);