ALTER TABLE `stockmgmt_stock_item_transaction` 
ADD COLUMN `order_id` INT NULL AFTER `uuid`,
ADD COLUMN `encounter_id` INT NULL AFTER `order_id`,
ADD INDEX `stockmgmt_stock_item_transaction_order_fk` (`order_id` ASC),
ADD INDEX `stockmgmt_stock_item_transaction_encounter_fk` (`encounter_id` ASC);
;
ALTER TABLE `stockmgmt_stock_item_transaction` 
ADD CONSTRAINT `stockmgmt_stock_item_transaction_order_fk`
  FOREIGN KEY (`order_id`)
  REFERENCES `orders` (`order_id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION,
ADD CONSTRAINT `stockmgmt_stock_item_transaction_encounter_fk`
  FOREIGN KEY (`encounter_id`)
  REFERENCES `encounter` (`encounter_id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;
