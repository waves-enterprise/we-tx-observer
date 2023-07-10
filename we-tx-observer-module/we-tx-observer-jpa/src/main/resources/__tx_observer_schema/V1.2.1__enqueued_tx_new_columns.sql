lock enqueued_tx;

alter table enqueued_tx
    add column tx_type int4;

update enqueued_tx
set tx_type = cast(body ->> 'type' as int4)
where tx_type is null;

alter table enqueued_tx
    alter column tx_type set not null;

alter table enqueued_tx
    add column available boolean;

update enqueued_tx
set available = false
where tx_type = 114
  and status = 'NEW'
  and available is null;

update enqueued_tx
set available = true
where available is null;

alter table enqueued_tx
    alter column available set not null;

---

alter table tx_queue_partition
    add column paused_on_tx_id varchar(255);



