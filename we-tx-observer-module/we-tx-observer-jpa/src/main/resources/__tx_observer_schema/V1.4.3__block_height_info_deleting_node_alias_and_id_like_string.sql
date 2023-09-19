alter table block_height_info
alter column id type varchar(255);

update block_height_info
set id = node_alias;

drop index if exists block_height_info_one_row;

alter table block_height_info
drop column node_alias;

create unique index if not exists block_height_info_one_row
    on block_height_info (id);