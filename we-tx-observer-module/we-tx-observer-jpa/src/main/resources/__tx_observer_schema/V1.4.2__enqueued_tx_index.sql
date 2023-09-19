CREATE INDEX ix__enqueued_tx_partition_id_timestamp_for_new
    ON enqueued_tx USING btree (partition_id, tx_timestamp)
    WHERE (status = 'NEW');