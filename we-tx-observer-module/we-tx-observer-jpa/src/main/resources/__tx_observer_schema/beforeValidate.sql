DO
$$
    BEGIN
        IF EXISTS
            (SELECT 1
             FROM information_schema.tables
             WHERE table_schema = 'tx_observer'
               AND table_name = 'flyway_schema_history'
            )
        THEN
            update tx_observer.flyway_schema_history
            set checksum = -1936963876
            where version = '1.2.1'
              and type = 'SQL'
              and checksum = -209010210;
        END IF;
    END
$$;
