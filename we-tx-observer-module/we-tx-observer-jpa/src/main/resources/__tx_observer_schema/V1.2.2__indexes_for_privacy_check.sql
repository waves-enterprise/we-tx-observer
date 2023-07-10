drop index tx_observer.ix__tx_queue_partition__priority_timestamp_partial;

CREATE INDEX ix__tx_queue_partition__priority_timestamp_partial ON tx_queue_partition USING btree (priority DESC, last_enqueued_tx_timestamp)
    WHERE (((last_read_tx_id)::text <> (last_enqueued_tx_id)::text) OR (last_read_tx_id IS NULL))
        and paused_on_tx_id is null;

CREATE INDEX ix_enqueued_not_available_privacy ON enqueued_tx USING btree (tx_timestamp) WHERE
    ((status)::text = 'NEW'::text) and tx_type = 114 and not available;