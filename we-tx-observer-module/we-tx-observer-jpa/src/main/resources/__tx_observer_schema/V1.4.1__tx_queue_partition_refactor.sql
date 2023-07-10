DROP INDEX ix__tx_queue_partition__priority_timestamp_partial;

ALTER TABLE tx_queue_partition drop column last_enqueued_tx_timestamp;
ALTER TABLE tx_queue_partition drop column last_read_tx_id;
ALTER TABLE tx_queue_partition drop column last_enqueued_tx_id;

CREATE INDEX if not exists ix__tx_queue_partition__priority_for_paused
    ON tx_queue_partition USING btree (priority DESC)
    WHERE (paused_on_tx_id IS NULL);
