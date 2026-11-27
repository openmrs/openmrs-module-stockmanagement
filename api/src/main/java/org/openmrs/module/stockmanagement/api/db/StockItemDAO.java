/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.db;

import java.util.*;
import org.openmrs.module.stockmanagement.api.model.*;
import org.openmrs.module.stockmanagement.api.dto.Result;
import org.openmrs.module.stockmanagement.api.search.StockItemSearch;

public interface StockItemDAO {
	
	StockItemPackagingUOM getStockItemPackagingUOMByUuid(String uuid);
	
	StockItemPackagingUOM saveStockItemPackagingUOM(StockItemPackagingUOM stockItemPackagingUOM);
	
	StockBatch getStockBatchByUuid(String uuid);
	
	StockBatch saveStockBatch(StockBatch stockBatch);
	
	StockItem getStockItemByUuid(String uuid);
	
	List<StockItem> getStockItemsByUuids(List<String> uuids);
	
	List<StockItem> getStockItemsByUuids(List<String> uuids, boolean includeVoided);
	
	StockItem saveStockItem(StockItem stockItem);
	
	StockSource getPreferredVendorByUuid(String uuid);
	
	List<StockItem> searchStockItemCommonName(String text, Boolean isDrugSearch, boolean includeAll, int maxItems);
	
	Result<StockItem> findStockItems(StockItemSearch filter);
	
	Result<StockItem> findStockItemEntities(StockItemSearch filter);
	
	List<StockItem> getExistingStockItemIds(Collection<StockItemSearch.ItemGroupFilter> stockItemFilters);
	
	void voidStockItemPackagingUOM(String uuid, String reason, int voidedBy);
	
	void voidStockItemReference(String uuid, String reason, int voidedBy);
	
	Result<StockItemPackagingUOM> findStockItemPackagingUOMs(StockItemSearch filter);
	
	StockBatch findStockBatch(StockItem stockItem, String batchNo, Date expiration);
	
	Result<StockBatch> findStockBatches(StockItemSearch filter);
	
	List<StockItemPackagingUOM> getStockItemPackagingUOMsByUuids(List<String> uuids);
	
	List<StockItem> getStockItems(Collection<Integer> stockItemIds);
	
	List<StockItemPackagingUOM> getStockItemPackagingUOMs(List<StockItemSearch.PackagingGroup> filters);
	
	List<StockItem> getStockItemByDrug(Integer drugId);
	
	List<StockItem> getStockItemByConcept(Integer conceptId);
	
	StockItemPackagingUOM getStockItemPackagingUOMByConcept(Integer stockItemId, Integer conceptId);
	
	StockItemReference getStockItemByReference(StockSource stockSource, String stockReferenceCode);
	
	StockItemReference saveStockItemReference(StockItemReference stockItemReference);
	
	StockItemReference getStockItemReferenceByUuid(String uuid);
	
	List<StockItemReference> getStockItemReferenceByStockItem(StockItem stockItem);
	
	StockItemPackagingUomReference saveStockItemPackagingUomReference(
	        StockItemPackagingUomReference stockItemPackagingUomReference);
	
	StockItemPackagingUomReference getStockItemPackagingUomReferenceByUuid(String uuid);
	
	List<StockItemPackagingUomReference> getStockItemPackagingUomReferenceByPackagingUom(
	        StockItemPackagingUOM stockItemPackagingUOM, boolean includeVoided);
	
	List<StockItemPackagingUomReference> getStockItemPackagingUomReferenceBySourceAndCode(StockSource stockSource,
	        String stockReferenceCode);
	
	void voidStockItemPackagingUomReference(String uuid, String reason, int voidedBy);
}
