CREATE INDEX IF NOT EXISTS ix__tx_track_info_pending ON tx_track_info USING btree (created)
    WHERE status = 'PENDING' and type in (103, 104);
