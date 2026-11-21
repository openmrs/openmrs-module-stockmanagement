/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.impl;

import org.openmrs.module.stockmanagement.api.StockItemService;
import org.openmrs.module.stockmanagement.api.db.StockItemDAO;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.module.stockmanagement.api.search.StockItemSearch;
import org.apache.commons.lang.StringUtils;
import org.openmrs.*;
import org.openmrs.api.APIException;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.dto.*;
import org.openmrs.module.stockmanagement.api.jobs.StockItemImportJob;
import org.openmrs.module.stockmanagement.api.model.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class StockItemServiceImpl extends BaseOpenmrsService implements StockItemService {
	
	private StockItemDAO dao;
	
	private StockManagementDao inventoryDao;
	
	/** Temporary inventory integration until inventory is extracted under O3-6306. */
	public void setInventoryDao(StockManagementDao inventoryDao) {
		this.inventoryDao = inventoryDao;
	}
	
	public void setDao(StockItemDAO dao) {
		this.dao = dao;
	}
	
	public Result<StockItem> findStockItemEntities(StockItemSearch filter) {
		return dao.findStockItemEntities(filter);
	}
	
	public List<StockItem> searchStockItemCommonName(String text, Boolean isDrugSearch, boolean includeAll, int maxItems) {
		return dao.searchStockItemCommonName(text, isDrugSearch, includeAll, maxItems);
	}
	
	public Result<StockItem> findStockItems(StockItemSearch filter) {
		return dao.findStockItems(filter);
	}
	
	public StockItem saveStockItem(StockItem stockItem) {
		dao.saveStockItem(stockItem);
		return stockItem;
	}
	
	public StockItem saveStockItem(StockItem stockItem, StockItemPackagingUOM stockItemPackagingUOM) {
		dao.saveStockItem(stockItem);
		dao.saveStockItemPackagingUOM(stockItemPackagingUOM);
		return stockItem;
	}
	
	public StockItemPackagingUOM getStockItemPackagingUOMByUuid(String uuid) {
		return dao.getStockItemPackagingUOMByUuid(uuid);
	}
	
	public StockItem getStockItemByUuid(String uuid) {
		return dao.getStockItemByUuid(uuid);
	}
	
	public Result<StockItemPackagingUOM> findStockItemPackagingUOMs(StockItemSearch filter) {
		return dao.findStockItemPackagingUOMs(filter);
	}
	
	public StockItemPackagingUOM saveStockItemPackagingUOM(StockItemPackagingUOM stockItemPackagingUOM) {
		dao.saveStockItemPackagingUOM(stockItemPackagingUOM);
		return stockItemPackagingUOM;
	}
	
	public StockBatch getStockBatchByUuid(String uuid) {
		return dao.getStockBatchByUuid(uuid);
	}
	
	public StockBatch findStockBatch(StockItem stockItem, String batchNo, Date expiration) {
		return dao.findStockBatch(stockItem, batchNo, expiration);
	}
	
	public Result<StockBatch> findStockBatches(StockItemSearch filter) {
		Result<StockBatch> result = dao.findStockBatches(filter);
		if (!result.getData().isEmpty() && !StringUtils.isBlank(filter.getLocationUuid())) {
			Location location = Context.getLocationService().getLocationByUuid(filter.getLocationUuid());
			if (location == null)
				return new Result<>(new ArrayList<>(), 0);
			
			Party party = Context.getService(StockManagementService.class).getPartyByLocation(location);
			if (party == null)
				return new Result<>(new ArrayList<>(), 0);
			
			List<StockItemInventorySearchFilter.ItemGroupFilter> itemsToSearch = result.getData().stream().map(p -> {
				StockItemInventorySearchFilter.ItemGroupFilter k = new StockItemInventorySearchFilter.ItemGroupFilter();
				k.setStockItemUuid(filter.getStockItemUuid());
				k.setStockBatchIds(Arrays.asList(p.getId()));
				k.setPartyIds(Arrays.asList(party.getId()));
				return k;
			}).collect(Collectors.toList());
			
			StockItemInventorySearchFilter searchFilter = new StockItemInventorySearchFilter();
			searchFilter.setItemGroupFilters(itemsToSearch);
			List<StockItemInventory> stockItemInventories = inventoryDao.getStockItemInventory(searchFilter, null).getData();
			result.setData(
			    result.getData().stream()
			            .filter(p -> stockItemInventories.stream().anyMatch(x -> x.getStockBatchId().equals(p.getId())
			                    && (!filter.getExcludeEmptyStock() || x.getQuantity().compareTo(BigDecimal.ZERO) > 0)))
			            .collect(Collectors.toList()));
		}
		
		return result;
	}
	
	public void voidStockItemPackagingUOM(String uuid, String reason, int voidedBy) {
		dao.voidStockItemPackagingUOM(uuid, reason, voidedBy);
	}
	
	public ImportResult importStockItems(Path file, boolean hasHeader) {
		StockItemImportJob importJob = new StockItemImportJob(file, hasHeader);
		importJob.execute();
		return (ImportResult) importJob.getResult();
	}
	
	public List<StockItem> getExistingStockItemIds(Collection<StockItemSearch.ItemGroupFilter> stockItemFilters) {
		return dao.getExistingStockItemIds(stockItemFilters);
	}
	
	public List<StockItem> getStockItems(Collection<Integer> stockItemIds) {
		return dao.getStockItems(stockItemIds);
	}
	
	public List<StockItemPackagingUOM> getStockItemPackagingUOMs(List<StockItemSearch.PackagingGroup> filters) {
		return dao.getStockItemPackagingUOMs(filters);
	}
	
	public List<StockItem> getStockItemByDrug(Integer drugId) {
		return dao.getStockItemByDrug(drugId);
	}
	
	public List<StockItem> getStockItemByConcept(Integer conceptId) {
		return dao.getStockItemByConcept(conceptId);
	}
	
	public StockItemPackagingUOM getStockItemPackagingUOMByConcept(Integer stockItemId, Integer conceptId) {
		return dao.getStockItemPackagingUOMByConcept(stockItemId, conceptId);
	}
	
	public StockItemPackagingUOM getStockItemPackagingUOMByConcept(Integer stockItemId, String conceptUuid) {
		Concept concept = Context.getConceptService().getConceptByUuid(conceptUuid);
		if (concept == null)
			return null;
		return getStockItemPackagingUOMByConcept(stockItemId, concept.getConceptId());
	}
	
	public StockItemPackagingUOM getStockItemPackagingUOMByConcept(String stockItemUuid, Integer conceptId) {
		Map<String, Integer> stockItemIds = dao.getStockItemsByUuids(Arrays.asList(stockItemUuid), true).stream()
		        .collect(Collectors.toMap(StockItem::getUuid, StockItem::getId));
		if (stockItemIds.isEmpty())
			return null;
		return getStockItemPackagingUOMByConcept(stockItemIds.values().iterator().next(), conceptId);
	}
	
	public StockItemPackagingUOM getStockItemPackagingUOMByConcept(String stockItemUuid, String conceptUuid) {
		Concept concept = Context.getConceptService().getConceptByUuid(conceptUuid);
		if (concept == null)
			return null;
		Map<String, Integer> stockItemIds = dao.getStockItemsByUuids(Arrays.asList(stockItemUuid), true).stream()
		        .collect(Collectors.toMap(StockItem::getUuid, StockItem::getId));
		if (stockItemIds.isEmpty())
			return null;
		return getStockItemPackagingUOMByConcept(stockItemIds.values().iterator().next(), concept.getId());
	}
	
	public StockItem getStockItemByReference(StockSource stockSource, String code) {
		StockItemReference stockItemReference = dao.getStockItemByReference(stockSource, code);
		if (stockItemReference != null) {
			return stockItemReference.getStockItem();
		}
		
		return null;
	}
	
	public StockItemReference saveStockItemReference(StockItemReference stockItemReference) {
		return dao.saveStockItemReference(stockItemReference);
	}
	
	public void voidStockItemReference(String uuid, String reason, Integer userId) {
		dao.voidStockItemReference(uuid, reason, userId);
	}
	
	public StockItemReference getStockItemReferenceByUuid(String uuid) {
		return dao.getStockItemReferenceByUuid(uuid);
	}
	
	public List<StockItemReference> getStockItemReferenceByStockItem(String uuid) {
		
		StockItem stockItem = getStockItemByUuid(uuid);
		if (stockItem != null) {
			return dao.getStockItemReferenceByStockItem(stockItem);
		} else {
			return null;
		}
	}
	
	public StockBatch saveStockBatch(StockBatch stockBatch) {
		return dao.saveStockBatch(stockBatch);
	}
	
	public List<StockItem> getStockItemsByUuids(List<String> uuids) {
		return dao.getStockItemsByUuids(uuids);
	}
	
	public List<StockItemPackagingUOM> getStockItemPackagingUOMsByUuids(List<String> uuids) {
		return dao.getStockItemPackagingUOMsByUuids(uuids);
	}
	
	@Override
	public StockItemPackagingUomReference saveStockItemPackagingUomReference(
	        StockItemPackagingUomReference stockItemPackagingUomReference) {
		if (stockItemPackagingUomReference == null) {
			throw new APIException("Stock item packaging UOM reference is required");
		}
		if (Boolean.TRUE.equals(stockItemPackagingUomReference.getVoided())) {
			throw new APIException("A voided stock item packaging UOM reference cannot be edited");
		}
		
		StockItemPackagingUOM stockItemPackagingUOM = stockItemPackagingUomReference.getStockItemPackagingUOM();
		StockItemPackagingUOM storedPackagingUOM = stockItemPackagingUOM == null ? null
		        : dao.getStockItemPackagingUOMByUuid(stockItemPackagingUOM.getUuid());
		if (stockItemPackagingUOM == null || stockItemPackagingUOM.getId() == null || storedPackagingUOM == null) {
			throw new APIException("Stock item packaging UOM is required and must exist");
		}
		//test the fetched row, not the caller's object: a detached copy may predate a void
		if (storedPackagingUOM.getVoided()) {
			throw new APIException("References cannot be created for a voided stock item packaging UOM");
		}
		
		StockSource referenceSource = stockItemPackagingUomReference.getReferenceSource();
		StockSource storedSource = referenceSource == null ? null
		        : inventoryDao.getStockSourceByUuid(referenceSource.getUuid());
		if (referenceSource == null || referenceSource.getId() == null || storedSource == null) {
			throw new APIException("Stock source is required and must exist");
		}
		//mirror the packaging UOM check on the fetched row: a reference of a voided source would
		//never resolve, yet its code would keep occupying the unique key on that source
		if (Boolean.TRUE.equals(storedSource.getVoided())) {
			throw new APIException("References cannot be created for a voided stock source");
		}
		
		if (stockItemPackagingUomReference.getStockReferenceCode() == null
		        || stockItemPackagingUomReference.getStockReferenceCode().trim().isEmpty()) {
			throw new APIException("Stock reference code is required");
		}
		stockItemPackagingUomReference.setStockReferenceCode(stockItemPackagingUomReference.getStockReferenceCode().trim());
		
		// voiding suffixes the stored code with the (voided <id>) format the entity owns, to free
		// the code for reuse, and stock_reference_code is a bounded column: refuse codes that
		// could no longer hold that suffix, so every active reference can still be voided
		if (stockItemPackagingUomReference.getStockReferenceCode().length() > StockItemPackagingUomReference
		        .maxSaveableCodeLength()) {
			throw new APIException("Stock reference code is too long: a code can be at most "
			        + StockItemPackagingUomReference.maxSaveableCodeLength()
			        + " characters so voiding can append its (voided <id>) suffix");
		}
		
		// Mirrors the database unique constraint on (stock_source_id, stock_reference_code);
		// voided rows are excluded because voiding suffixes their stored code, so only active
		// references collide. The query must not auto-flush the pending edits or the database
		// constraint fires before this friendly duplicate check runs. Dormant references (a
		// voided packaging UOM or source) still hold the unique key, so they must collide too —
		// the message names the reference that holds the code, which the resolve and search
		// queries stop finding
		List<StockItemPackagingUomReference> matches = dao.getStockItemPackagingUomReferenceBySourceAndCode(
		    stockItemPackagingUomReference.getReferenceSource(), stockItemPackagingUomReference.getStockReferenceCode());
		if (!matches.isEmpty() && !matches.get(0).getId().equals(stockItemPackagingUomReference.getId())) {
			throw new APIException(
			        "Stock item packaging UOM reference code " + stockItemPackagingUomReference.getStockReferenceCode()
			                + " already exists for the given stock source (reference " + matches.get(0).getUuid() + ")");
		}
		
		return dao.saveStockItemPackagingUomReference(stockItemPackagingUomReference);
	}
	
	@Override
	public void voidStockItemPackagingUomReference(String uuid, String reason, int userId) {
		if (uuid == null || uuid.trim().isEmpty()) {
			throw new APIException("Stock item packaging UOM reference uuid is required");
		}
		dao.voidStockItemPackagingUomReference(uuid, reason, userId);
	}
	
	@Override
	public StockItemPackagingUomReference getStockItemPackagingUomReferenceByUuid(String uuid) {
		return dao.getStockItemPackagingUomReferenceByUuid(uuid);
	}
	
	@Override
	public List<StockItemPackagingUomReference> getStockItemPackagingUomReferencesByPackagingUom(
	        String stockItemPackagingUomUuid, boolean includeVoided) {
		StockItemPackagingUOM stockItemPackagingUOM = dao.getStockItemPackagingUOMByUuid(stockItemPackagingUomUuid);
		if (stockItemPackagingUOM == null) {
			return Collections.emptyList();
		}
		return dao.getStockItemPackagingUomReferenceByPackagingUom(stockItemPackagingUOM, includeVoided);
	}
	
	@Override
	public StockItemPackagingUOM getStockItemPackagingUomByReference(StockSource stockSource, String stockReferenceCode) {
		if (stockSource == null || stockReferenceCode == null || stockReferenceCode.trim().isEmpty()) {
			throw new APIException("Stock source and stock reference code are required");
		}
		List<StockItemPackagingUomReference> matches = getResolvableReferences(stockSource, stockReferenceCode.trim());
		if (matches.isEmpty()) {
			return null;
		}
		if (matches.size() > 1) {
			throw new APIException("Multiple stock item packaging UOM references found for stock reference code "
			        + stockReferenceCode + " and the given stock source");
		}
		return matches.get(0).getStockItemPackagingUOM();
	}
	
	@Override
	public List<StockItemPackagingUomReference> getStockItemPackagingUomReferencesBySourceAndCode(String stockSourceUuid,
	        String stockReferenceCode) {
		StockSource stockSource = inventoryDao.getStockSourceByUuid(stockSourceUuid);
		if (stockSource == null) {
			//a stale source uuid is just an empty result, matching the stale packaging uuid lookup
			return Collections.emptyList();
		}
		if (stockReferenceCode == null || stockReferenceCode.trim().isEmpty()) {
			throw new APIException("Stock reference code is required");
		}
		return getResolvableReferences(stockSource, stockReferenceCode.trim());
	}
	
	//a voided packaging UOM or source must not resolve: its references stay stored but dormant,
	//and a client must not be handed a stockItemPackagingUomUuid that the UOM resource answers
	//404 for
	private List<StockItemPackagingUomReference> getResolvableReferences(StockSource stockSource,
	        String stockReferenceCode) {
		return dao.getStockItemPackagingUomReferenceBySourceAndCode(stockSource, stockReferenceCode).stream()
		        .filter(r -> !Boolean.TRUE.equals(r.getStockItemPackagingUOM().getVoided())
		                && !Boolean.TRUE.equals(r.getReferenceSource().getVoided()))
		        .collect(Collectors.toList());
	}
}
