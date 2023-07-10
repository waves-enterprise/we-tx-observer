do
$$
    begin
        insert into smart_contract_info
        select id,
               image_hash,
               image,
               version,
               contract_name,
               sender,
               created,
               modified
        from public.smart_contract_info;
    exception
        when undefined_table then null;
    end
$$;

do
$$
    begin
        insert into tx_track_info
        select id,
               contract_id,
               status,
               type,
               body,
               errors,
               meta,
               created,
               modified,
               user_id
        from public.tx_track_info;
    exception
        when undefined_table then null;
    end
$$;

do
$$
    begin
        insert into tx_track_business_object_info
        select id, type, tx_track_info_id
        from public.tx_track_business_object_info;
    exception
        when undefined_table then null;
    end
$$;
