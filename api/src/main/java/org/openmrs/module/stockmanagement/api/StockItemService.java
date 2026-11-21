/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api;

import java.util.*;
import java.nio.file.Path;
import org.openmrs.api.OpenmrsService;
import org.openmrs.annotation.Authorized;
import org.springframework.transaction.annotation.Transactional;
import org.openmrs.module.stockmanagement.api.model.*;
import org.openmrs.module.stockmanagement.api.dto.Result;
import org.openmrs.module.stockmanagement.api.dto.ImportResult;
import org.openmrs.module.stockmanagement.api.search.StockItemSearch;

/** Stock items, packaging units, vendor references, batches and bulk item import. */
public interface StockItemService extends OpenmrsService {
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	Result<StockItem> findStockItemEntities(StockItemSearch filter);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	List<StockItem> searchStockItemCommonName(String text, Boolean isDrugSearch, boolean includeAll, int maxItems);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	Result<StockItem> findStockItems(StockItemSearch filter);
	
	@Transactional
	@Authorized(Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE)
	StockItem saveStockItem(StockItem stockItem);
	
	@Transactional
	@Authorized(Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE)
	StockItem saveStockItem(StockItem stockItem, StockItemPackagingUOM stockItemPackagingUOM);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	StockItemPackagingUOM getStockItemPackagingUOMByUuid(String uuid);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	StockItem getStockItemByUuid(String uuid);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	Result<StockItemPackagingUOM> findStockItemPackagingUOMs(StockItemSearch filter);
	
	@Transactional
	@Authorized(Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE)
	StockItemPackagingUOM saveStockItemPackagingUOM(StockItemPackagingUOM stockItemPackagingUOM);
	
	@Transactional(readOnly = true)
	@Authorized(value = { Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS,
	        Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE }, requireAll = false)
	StockBatch getStockBatchByUuid(String uuid);
	
	@Transactional(readOnly = true)
	@Authorized(value = { Privileges.APP_STOCKMANAGEMENT_STOCKITEMS,
	        Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE }, requireAll = false)
	StockBatch findStockBatch(StockItem stockItem, String batchNo, Date expiration);
	
	@Transactional(readOnly = true)
	@Authorized(value = { Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS,
	        Privileges.APP_STOCKMANAGEMENT_STOCKITEMS }, requireAll = false)
	Result<StockBatch> findStockBatches(StockItemSearch filter);
	
	@Transactional
	@Authorized(Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE)
	void voidStockItemPackagingUOM(String uuid, String reason, int voidedBy);
	
	@Authorized(Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE)
	ImportResult importStockItems(Path file, boolean hasHeader);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	List<StockItem> getExistingStockItemIds(Collection<StockItemSearch.ItemGroupFilter> stockItemFilters);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	List<StockItem> getStockItems(Collection<Integer> stockItemIds);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	List<StockItemPackagingUOM> getStockItemPackagingUOMs(List<StockItemSearch.PackagingGroup> filters);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	List<StockItem> getStockItemByDrug(Integer drugId);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	List<StockItem> getStockItemByConcept(Integer conceptId);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	StockItemPackagingUOM getStockItemPackagingUOMByConcept(Integer stockItemId, Integer conceptId);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	StockItemPackagingUOM getStockItemPackagingUOMByConcept(Integer stockItemId, String conceptUuid);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	StockItemPackagingUOM getStockItemPackagingUOMByConcept(String stockItemUuid, Integer conceptId);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	StockItemPackagingUOM getStockItemPackagingUOMByConcept(String stockItemUuid, String conceptUuid);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	StockItem getStockItemByReference(StockSource stockSource, String code);
	
	@Transactional
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	StockItemReference saveStockItemReference(StockItemReference stockItemReference);
	
	@Transactional
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	void voidStockItemReference(String uuid, String reason, Integer userId);
	
	@Transactional
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	StockItemReference getStockItemReferenceByUuid(String uuid);
	
	@Transactional
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	List<StockItemReference> getStockItemReferenceByStockItem(String uuid);
	
	@Transactional
	@Authorized(value = { Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE,
	        Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE }, requireAll = false)
	StockBatch saveStockBatch(StockBatch stockBatch);
	
	@Transactional(readOnly = true)
	@Authorized(value = { Privileges.APP_STOCKMANAGEMENT_STOCKITEMS,
	        Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE }, requireAll = false)
	List<StockItem> getStockItemsByUuids(List<String> uuids);
	
	@Transactional(readOnly = true)
	@Authorized(value = { Privileges.APP_STOCKMANAGEMENT_STOCKITEMS,
	        Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE }, requireAll = false)
	List<StockItemPackagingUOM> getStockItemPackagingUOMsByUuids(List<String> uuids);
	
	@Transactional
	@Authorized(value = { Privileges.APP_STOCKMANAGEMENT_STOCKITEMS,
	        Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE }, requireAll = true)
	StockItemPackagingUomReference saveStockItemPackagingUomReference(
	        StockItemPackagingUomReference stockItemPackagingUomReference);
	
	@Transactional
	@Authorized(value = { Privileges.APP_STOCKMANAGEMENT_STOCKITEMS,
	        Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE }, requireAll = true)
	void voidStockItemPackagingUomReference(String uuid, String reason, int userId);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	StockItemPackagingUomReference getStockItemPackagingUomReferenceByUuid(String uuid);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	List<StockItemPackagingUomReference> getStockItemPackagingUomReferencesByPackagingUom(String stockItemPackagingUomUuid,
	        boolean includeVoided);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	StockItemPackagingUOM getStockItemPackagingUomByReference(StockSource stockSource, String stockReferenceCode);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS)
	List<StockItemPackagingUomReference> getStockItemPackagingUomReferencesBySourceAndCode(String stockSourceUuid,
	        String stockReferenceCode);
}
