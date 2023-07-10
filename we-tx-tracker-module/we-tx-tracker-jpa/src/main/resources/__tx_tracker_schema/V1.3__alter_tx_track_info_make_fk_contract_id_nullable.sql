ALTER TABLE tx_track_info
    ALTER COLUMN contract_id DROP NOT NULL;

ALTER TABLE tx_track_info
    ADD CONSTRAINT contract_id_null_check
        CHECK (CASE
                   WHEN type IN (103, 104, 106, 107) THEN contract_id IS NOT NULL
                   ELSE contract_id IS NULL
            END);
