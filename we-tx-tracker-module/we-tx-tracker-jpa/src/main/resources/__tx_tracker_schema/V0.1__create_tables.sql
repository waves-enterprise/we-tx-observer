--region SmartContractInfo
create table if not exists smart_contract_info
(
    id            varchar(255) not null
        constraint smart_contract_info_pkey
            primary key,
    image_hash    varchar(255) not null,
    image         varchar(255) not null,
    version       integer      not null,
    contract_name varchar(255) not null,
    sender        varchar(255) not null,
    created       timestamp,
    modified      timestamp
);

create index if not exists smart_contract_contract_name_idx
    on smart_contract_info (contract_name);

create index if not exists smart_contract_info_image_hash_idx
    on smart_contract_info (image_hash);

create index if not exists smart_contract_info_image_idx
    on smart_contract_info (image);

create index if not exists smart_contract_info_sender_idx
    on smart_contract_info (sender);
--endregion

--region TxTrackInfo
create table if not exists tx_track_info
(
    id          varchar(255) not null
        constraint tx_track_info_pkey
            primary key,
    contract_id varchar(255) not null
        constraint fk_contract_id
            references smart_contract_info,
    status      varchar(255) not null,
    type        integer      not null,
    body        jsonb        not null,
    errors      jsonb,
    meta        jsonb        not null,
    created     timestamp,
    modified    timestamp,
    user_id     uuid
);

create index if not exists tx_track_info_contract_id_idx
    on tx_track_info (contract_id);
--endregion

--region TxTrackBusinessObjectInfo
create table if not exists tx_track_business_object_info
(
    id               varchar(255) not null
        constraint tx_track_business_object_info_pkey
            primary key,
    type             varchar(255) not null,
    tx_track_info_id varchar(255) not null
        constraint fk_tx_track_info_id
            references tx_track_info
);
--endregion
