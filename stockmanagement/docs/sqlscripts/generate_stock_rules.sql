/* INSERT INTO `stockmgmt`.`stockmgmt_stock_rule`(`stock_item_id`,`name`,`location_id`,`quantity`,`stock_item_packaging_uom_id`,`enabled`,
 `evaluation_frequency`,`action_frequency`,`creator`,`date_created`,`uuid`,`alert_role`,
 `mail_role`,`enable_descendants`)
select si.stock_item_id, concat('1000-', l.name, '-', roles.role) , l.location_id, 1000, sipu.stock_item_packaging_uom_id, 1,
60,1440,1,now(), uuid(), roles.role, 'Stock Management Base Role', 1
from stockmgmt_stock_item si join
	 stockmgmt_stock_item_packaging_uom sipu on si.stock_item_id = sipu.stock_item_id,
     (select 'Inventory Clerk' as role union select 'Inventory Manager') as roles,
     (select location_id, name  from location where lower(name) like '%pharm%' or lower(name) like '%main store%'  or lower(name) like '%dispen%') as l
*/     

select count(*) from stockmgmt_stock_rule;
update stockmgmt_stock_rule set enabled = 1;