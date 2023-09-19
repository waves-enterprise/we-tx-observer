create or replace view last_unsuccessful_tx_for_business_object as
select distinct on (link.business_object_info_id) link.business_object_info_id as business_object_info_id,
                                                  tx.id                        as tx_id
from tx_track_info tx
         inner join track_info_business_object_info link on tx.id = link.track_info_id
where tx.status != 'SUCCESS'
order by link.business_object_info_id, tx.modified desc