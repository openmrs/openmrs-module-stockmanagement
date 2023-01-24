delete from stockmgmt_stock_item_packaging_uom where stock_item_id in 
(select ssi.stock_item_id from stockmgmt_stock_item ssi 
where drug_id in (select d.drug_id from drug d where lower(d.name) like 'l%'));
delete from stockmgmt_stock_batch where stock_item_id in (select ssi.stock_item_id from stockmgmt_stock_item ssi 
where drug_id in (select d.drug_id from drug d where lower(d.name) like 'l%'));
delete from stockmgmt_stock_item
where drug_id in (select d.drug_id from drug d where lower(d.name) like 'l%');
select * from stockmgmt_stock_item ssi 
where drug_id in (select d.drug_id from drug d where lower(d.name) like 'l%');
select * from drug where lower(name) like 'l%';