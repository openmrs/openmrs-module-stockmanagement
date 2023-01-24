


-- INSERT INTO `stockmgmt`.`stockmgmt_stock_item_packaging_uom` (`stock_item_id`, `packaging_uom_id`, `factor`, `creator`, `date_created`, `uuid`)
-- SELECT si.stock_item_id, (SELECT answer_concept FROM concept_answer where concept_id=166789 AND answer_concept <> sipu.packaging_uom_id order by rand() limit 1), 
-- FLOOR( RAND() * (50-1) + 50),1,now(),uuid()
-- FROM stockmgmt_stock_item si left join 
-- 	 `stockmgmt_stock_item_packaging_uom` sipu on si.stock_item_id=sipu.stock_item_id
-- where sipu.stock_item_packaging_uom_id is null




INSERT INTO `ugandaemr`.`stockmgmt_stock_item_packaging_uom` (`stock_item_id`, `packaging_uom_id`, `factor`, `creator`, `date_created`, `uuid`)
SELECT si.stock_item_id, 1513,1,1,now(),uuid()
FROM stockmgmt_stock_item si left join 
	 `stockmgmt_stock_item_packaging_uom` sipu on si.stock_item_id=sipu.stock_item_id
where sipu.stock_item_packaging_uom_id is null;



INSERT INTO `ugandaemr`.`stockmgmt_stock_item_packaging_uom` (`stock_item_id`, `packaging_uom_id`, `factor`, `creator`, `date_created`, `uuid`)
SELECT si.stock_item_id,166790, 10,1,now(),uuid()
FROM stockmgmt_stock_item si left join 
	 `stockmgmt_stock_item_packaging_uom` sipu on si.stock_item_id=sipu.stock_item_id
where sipu.stock_item_packaging_uom_id is not null;