do
$$
    begin
        insert into block_height_info
        select id,
               created_timestamp,
               coalesce((select min(block_height) - 2 from public.enqueued_tx where status = 'NEW'),
                        current_height - 2),
               node_alias,
               update_timestamp,
               version
        from public.block_height_info
        order by update_timestamp desc
        limit 1;
    exception
        when undefined_table then null;
    end
$$;

do
$$
    begin
        insert into tx_queue_partition
        select id,
               priority,
               last_enqueued_tx_timestamp,
               last_enqueued_tx_id,
               last_read_tx_id,
               created,
               modified
        from public.tx_queue_partition;
    exception
        when undefined_table then null;
    end
$$;

do
$$
    begin
        insert into enqueued_tx
        select id,
               status,
               body,
               block_height,
               position_in_block,
               tx_timestamp,
               created,
               modified,
               partition_id
        from public.enqueued_tx;
    exception
        when undefined_table then null;
    end
$$;
