select ssi.drug_id, c.concept_id,ssi.has_expiration,ssi.dispensing_unit_id, sipu.packaging_uom_id, sipu.factor,
ssi.common_name, ssi.acronym, sss.name,ssi.reorder_level, ro.packaging_uom_id,
ssi.purchase_price, pu.packaging_uom_id
from stockmgmt_stock_item ssi left join
	 drug d on ssi.drug_id = d.drug_id left join
     concept c on ssi.concept_id = c.concept_id left join 
     stockmgmt_stock_item_packaging_uom sipu on ssi.dispensing_unit_packaging_uom_id = sipu.stock_item_packaging_uom_id left join 
     stockmgmt_stock_source sss on ssi.preferred_vendor_id=sss.stock_source_id left join 
     stockmgmt_stock_item_packaging_uom ro on ssi.reorder_level_uom_id = ro.stock_item_packaging_uom_id left join 
     stockmgmt_stock_item_packaging_uom pu on ssi.purchase_price_uom_id = pu.stock_item_packaging_uom_id 

