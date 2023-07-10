create table if not exists track_info_business_object_info
(
    track_info_id           varchar(255) not null
        constraint fk_track_info_business_object_info_track_info_id
            references tx_track_info,
    business_object_info_id varchar(255) not null
        constraint fk_track_info_business_object_info_business_object_info_id
            references tx_track_business_object_info,
    constraint track_info_business_object_info_pkey
        primary key (track_info_id, business_object_info_id)
);

insert into track_info_business_object_info
select tx_track_info_id, id
from tx_track_business_object_info;

alter table tx_track_business_object_info
    drop constraint fk_tx_track_info_id;
alter table tx_track_business_object_info
    drop column tx_track_info_id;

