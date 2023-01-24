select d.drug_id, d.concept_id,1 expires, 162399 dispensing_unit_id, 162399 packaging_uom_id, 1 factor,
null common_name,null acronym, 'Uganda Medical Stores', 1000 reorder_level, 162399 packaging_uom_id,
null purchase_price, null packaging_uom_id
from drug d left join stockmgmt_stock_item ssi on ssi.drug_id = d.drug_id left join
     concept c on ssi.concept_id = c.concept_id left join 
     stockmgmt_stock_item_packaging_uom sipu on ssi.dispensing_unit_packaging_uom_id = sipu.stock_item_packaging_uom_id left join 
     stockmgmt_stock_source sss on ssi.preferred_vendor_id=sss.stock_source_id left join 
     stockmgmt_stock_item_packaging_uom ro on ssi.reorder_level_uom_id = ro.stock_item_packaging_uom_id left join 
     stockmgmt_stock_item_packaging_uom pu on ssi.purchase_price_uom_id = pu.stock_item_packaging_uom_id 
where ssi.stock_item_id is null     

