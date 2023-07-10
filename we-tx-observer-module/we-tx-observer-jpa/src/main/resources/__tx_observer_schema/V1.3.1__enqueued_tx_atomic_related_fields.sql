alter table enqueued_tx
    add column position_in_atomic integer;

alter table enqueued_tx
    add column atomic_tx_id varchar(255);