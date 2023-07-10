create table shedlock
(
    name varchar(64) not null
        constraint shedlock_pkey
            primary key,
    lock_until timestamp(3),
    locked_at timestamp(3),
    locked_by varchar(255)
);
