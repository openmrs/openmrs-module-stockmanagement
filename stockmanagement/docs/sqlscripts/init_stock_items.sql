SELECT * FROM stockmgmt.drug where drug_id < 5;
select * from concept_name where concept_id in (SELECT concept_id FROM stockmgmt.drug where drug_id < 5);

INSERT INTO `ugandaemr`.`stockmgmt_stock_item`
(
`drug_id`,
`concept_id`,
`has_expiration`,
`creator`,
`date_created`,
`uuid`)
SELECT d.drug_id, d.concept_id, true, 1,now(),uuid() FROM stockmgmt.drug d;

INSERT INTO `ugandaemr`.`stockmgmt_stock_item`
(
`drug_id`,
`concept_id`,
`has_expiration`,
`creator`,
`date_created`,
`uuid`)
SELECT null,c.concept_id,0,1,now(),uuid() FROM concept c where c.class_id=28