/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.web.resource;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.*;
import org.openmrs.module.stockmanagement.api.model.*;
import org.openmrs.module.stockmanagement.api.dto.*;
import org.openmrs.module.stockmanagement.api.search.StockItemSearch;
import org.openmrs.module.stockmanagement.api.utils.StockItemFields;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Keeps the existing REST representation independent of persistence queries. */
public class StockItemRestMapper {
	
	public static StockItemDTO item(StockItem item) {
		if (item == null)
			return null;
		StockItemDTO dto = new StockItemDTO();
		dto.setUuid(item.getUuid());
		dto.setId(item.getId());
		dto.setDrugId((item.getDrug() == null ? null : item.getDrug().getDrugId()));
		dto.setDrugUuid((item.getDrug() == null ? null : item.getDrug().getUuid()));
		dto.setDrugName((item.getDrug() == null ? null : item.getDrug().getName()));
		dto.setConceptId((item.getConcept() == null ? null : item.getConcept().getConceptId()));
		dto.setConceptUuid((item.getConcept() == null ? null : item.getConcept().getUuid()));
		dto.setHasExpiration(item.getHasExpiration());
		dto.setPreferredVendorId((item.getPreferredVendor() == null ? null : item.getPreferredVendor().getId()));
		dto.setPreferredVendorUuid((item.getPreferredVendor() == null ? null : item.getPreferredVendor().getUuid()));
		dto.setPreferredVendorName((item.getPreferredVendor() == null ? null : item.getPreferredVendor().getName()));
		dto.setPurchasePrice(item.getPurchasePrice());
		dto.setPurchasePriceUoMId((item.getPurchasePriceUoM() == null ? null : item.getPurchasePriceUoM().getId()));
		dto.setPurchasePriceUoMUuid((item.getPurchasePriceUoM() == null ? null : item.getPurchasePriceUoM().getUuid()));
		dto.setPurchasePriceUoMFactor((item.getPurchasePriceUoM() == null ? null : item.getPurchasePriceUoM().getFactor()));
		dto.setPurchasePriceConceptId(
		    (item.getPurchasePriceUoM() == null || item.getPurchasePriceUoM().getPackagingUom() == null ? null
		            : item.getPurchasePriceUoM().getPackagingUom().getConceptId()));
		dto.setDispensingUnitId((item.getDispensingUnit() == null ? null : item.getDispensingUnit().getConceptId()));
		dto.setDispensingUnitUuid((item.getDispensingUnit() == null ? null : item.getDispensingUnit().getUuid()));
		dto.setDispensingUnitPackagingUoMId(
		    (item.getDispensingUnitPackagingUoM() == null ? null : item.getDispensingUnitPackagingUoM().getId()));
		dto.setDispensingUnitPackagingUoMUuid(
		    (item.getDispensingUnitPackagingUoM() == null ? null : item.getDispensingUnitPackagingUoM().getUuid()));
		dto.setDispensingUnitPackagingUoMFactor(
		    (item.getDispensingUnitPackagingUoM() == null ? null : item.getDispensingUnitPackagingUoM().getFactor()));
		dto.setDispensingUnitPackagingConceptId(
		    (item.getDispensingUnitPackagingUoM() == null || item.getDispensingUnitPackagingUoM().getPackagingUom() == null
		            ? null
		            : item.getDispensingUnitPackagingUoM().getPackagingUom().getConceptId()));
		dto.setDefaultStockOperationsUoMId(
		    (item.getDefaultStockOperationsUoM() == null ? null : item.getDefaultStockOperationsUoM().getId()));
		dto.setDefaultStockOperationsUoMUuid(
		    (item.getDefaultStockOperationsUoM() == null ? null : item.getDefaultStockOperationsUoM().getUuid()));
		dto.setDefaultStockOperationsUoMFactor(
		    (item.getDefaultStockOperationsUoM() == null ? null : item.getDefaultStockOperationsUoM().getFactor()));
		dto.setDefaultStockOperationsConceptId(
		    (item.getDefaultStockOperationsUoM() == null || item.getDefaultStockOperationsUoM().getPackagingUom() == null
		            ? null
		            : item.getDefaultStockOperationsUoM().getPackagingUom().getConceptId()));
		dto.setCommonName(item.getCommonName());
		dto.setAcronym(item.getAcronym());
		dto.setReorderLevel(item.getReorderLevel());
		dto.setReorderLevelUoMId((item.getReorderLevelUOM() == null ? null : item.getReorderLevelUOM().getId()));
		dto.setReorderLevelUoMUuid((item.getReorderLevelUOM() == null ? null : item.getReorderLevelUOM().getUuid()));
		dto.setReorderLevelUoMFactor((item.getReorderLevelUOM() == null ? null : item.getReorderLevelUOM().getFactor()));
		dto.setCategoryId((item.getCategory() == null ? null : item.getCategory().getConceptId()));
		dto.setCategoryUuid((item.getCategory() == null ? null : item.getCategory().getUuid()));
		dto.setReorderLevelConceptId(
		    (item.getReorderLevelUOM() == null || item.getReorderLevelUOM().getPackagingUom() == null ? null
		            : item.getReorderLevelUOM().getPackagingUom().getConceptId()));
		dto.setCreator((item.getCreator() == null ? null : item.getCreator().getUserId()));
		dto.setDateCreated(item.getDateCreated());
		dto.setExpiryNotice(item.getExpiryNotice());
		dto.setVoided(item.getVoided());
		dto.setConceptName(StockItemFields.name(item.getConcept(), false));
		dto.setDispensingUnitName(StockItemFields.name(item.getDispensingUnit(), false));
		dto.setPurchasePriceUoMName(StockItemFields
		        .name((item.getPurchasePriceUoM() == null ? null : item.getPurchasePriceUoM().getPackagingUom()), false));
		dto.setDispensingUnitPackagingUoMName(StockItemFields.name(
		    (item.getDispensingUnitPackagingUoM() == null ? null : item.getDispensingUnitPackagingUoM().getPackagingUom()),
		    false));
		dto.setDefaultStockOperationsUoMName(StockItemFields.name(
		    (item.getDefaultStockOperationsUoM() == null ? null : item.getDefaultStockOperationsUoM().getPackagingUom()),
		    false));
		dto.setReorderLevelUoMName(StockItemFields
		        .name((item.getReorderLevelUOM() == null ? null : item.getReorderLevelUOM().getPackagingUom()), false));
		dto.setCategoryName(StockItemFields.name(item.getCategory(), false));
		if (item.getCreator() != null && item.getCreator().getPerson() != null) {
			org.openmrs.PersonName name = item.getCreator().getPerson().getNames().stream()
			        .min(Comparator.comparing(PersonName::getId)).orElse(null);
			if (name != null) {
				dto.setCreatorGivenName(name.getGivenName());
				dto.setCreatorFamilyName(name.getFamilyName());
			}
		}
		return dto;
	}
	
	public static StockItemPackagingUOMDTO packaging(StockItemPackagingUOM unit) {
		StockItemPackagingUOMDTO dto = new StockItemPackagingUOMDTO();
		dto.setId(unit.getId());
		dto.setUuid(unit.getUuid());
		dto.setVoided(unit.getVoided());
		dto.setFactor(unit.getFactor());
		dto.setStockItemId(unit.getStockItem().getId());
		dto.setStockItemUuid(unit.getStockItem().getUuid());
		dto.setPackagingUomId(unit.getPackagingUom().getId());
		dto.setPackagingUomUuid(unit.getPackagingUom().getUuid());
		dto.setPackagingUomName(StockItemFields.name(unit.getPackagingUom(), true));
		dto.setIsDefaultStockOperationsUoM(StockItemFields.isDefaultUnit(unit));
		dto.setIsDispensingUnit(StockItemFields.isDispensingUnit(unit));
		return dto;
	}
	
	public static StockBatchDTO batch(StockBatch batch) {
		StockBatchDTO dto = new StockBatchDTO();
		dto.setId(batch.getId());
		dto.setUuid(batch.getUuid());
		dto.setStockItemUuid(batch.getStockItem().getUuid());
		dto.setBatchNo(batch.getBatchNo());
		dto.setExpiration(batch.getExpiration());
		dto.setExpiryNotificationDate(batch.getExpiryNotificationDate());
		dto.setVoided(batch.getVoided());
		return dto;
	}
	
	private static <E, D> Result<D> map(Result<E> source, java.util.function.Function<E, D> mapper) {
		Result<D> result = new Result<>();
		source.copyPagingInfoTo(result);
		result.setData(source.getData().stream().map(mapper).collect(Collectors.toList()));
		return result;
	}
	
	public Result<StockItemDTO> findStockItems(StockItemSearch filter) {
		return map(service().findStockItems(filter), StockItemRestMapper::item);
	}
	
	public Result<StockItemPackagingUOMDTO> findStockItemPackagingUOMs(StockItemSearch filter) {
		return map(service().findStockItemPackagingUOMs(filter), unit -> {
			StockItemPackagingUOMDTO dto = packaging(unit);
			if (filter.includingDispensingUnit() && unit.getStockItem().getDispensingUnit() != null) {
				dto.setStockItemDispensingUnitId(unit.getStockItem().getDispensingUnit().getId());
				dto.setStockItemDispensingUnitName(StockItemFields.name(unit.getStockItem().getDispensingUnit(), false));
			}
			return dto;
		});
	}
	
	public Result<StockBatchDTO> findStockBatches(StockItemSearch filter) {
		return map(service().findStockBatches(filter), StockItemRestMapper::batch);
	}
	
	private StockItemService service() {
		return Context.getService(StockItemService.class);
	}
	
	private void invalidRequest(String key) {
		throw new StockManagementException(Context.getMessageSourceService().getMessage(key));
	}
	
	public StockItem saveStockItem(StockItemDTO stockItemDTO) {
		Context.requirePrivilege(Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE);
		return writeTransaction().execute(status -> saveItem(stockItemDTO));
	}
	
	private TransactionTemplate writeTransaction() {
		// Mapping updates managed entities, so validation and persistence must share a transaction.
		return new TransactionTemplate(
		        Context.getRegisteredComponent("transactionManager", PlatformTransactionManager.class));
	}
	
	private StockItem saveItem(StockItemDTO stockItemDTO) {
		StockItem stockItem = null;
		boolean isNew = false;
		if (stockItemDTO.getUuid() == null) {
			isNew = true;
			stockItem = new StockItem();
			stockItem.setCreator(Context.getAuthenticatedUser());
			stockItem.setDateCreated(new Date());
			
			if (!StringUtils.isBlank(stockItemDTO.getDrugUuid())) {
				Drug drug = Context.getConceptService().getDrugByUuid(stockItemDTO.getDrugUuid());
				if (drug != null) {
					StockItemSearch filter = new StockItemSearch();
					filter.setDrugId(drug.getDrugId());
					filter.setIsDrug(true);
					filter.setIncludeVoided(true);
					filter.setStartIndex(0);
					filter.setLimit(1);
					Result<StockItemDTO> stockItemResult = findStockItems(filter);
					if (!stockItemResult.getData().isEmpty()) {
						invalidRequest("stockmanagement.stockitem.drugexists");
					}
					stockItem.setDrug(drug);
					stockItem.setIsDrug(true);
					stockItem.setConcept(drug.getConcept());
				} else {
					invalidRequest("stockmanagement.stockitem.drugnoexist");
				}
			} else if (!StringUtils.isBlank(stockItemDTO.getConceptUuid())) {
				Concept concept = Context.getConceptService().getConceptByUuid(stockItemDTO.getConceptUuid());
				if (concept != null) {
					StockItemSearch filter = new StockItemSearch();
					filter.setIsDrug(false);
					filter.setConceptId(concept.getConceptId());
					filter.setIncludeVoided(true);
					filter.setStartIndex(0);
					filter.setLimit(1);
					Result<StockItemDTO> stockItemResult = findStockItems(filter);
					if (!stockItemResult.getData().isEmpty()) {
						invalidRequest("stockmanagement.stockitem.conceptexists");
					}
					
					stockItem.setIsDrug(false);
					stockItem.setConcept(concept);
				} else {
					invalidRequest("stockmanagement.stockitem.conceptnoexist");
				}
			} else {
				invalidRequest("stockmanagement.stockitem.drugorconceptrequired");
			}
		} else {
			stockItem = service().getStockItemByUuid(stockItemDTO.getUuid());
			if (stockItem == null) {
				invalidRequest("stockmanagement.stockitem.notexists");
			}
			if (stockItem.getIsDrug() == null) {
				stockItem.setIsDrug(stockItem.getDrug() != null);
			}
			stockItem.setChangedBy(Context.getAuthenticatedUser());
			stockItem.setDateChanged(new Date());
		}
		
		stockItem.setCommonName(stockItemDTO.getCommonName());
		stockItem.setAcronym(stockItemDTO.getAcronym());
		stockItem.setHasExpiration(stockItemDTO.getHasExpiration());
		stockItem.setExpiryNotice(stockItemDTO.getExpiryNotice());
		
		if (!StringUtils.isBlank(stockItemDTO.getPreferredVendorUuid())) {
			StockSource stockSource = Context.getService(StockManagementService.class)
			        .getStockSourceByUuid(stockItemDTO.getPreferredVendorUuid());
			if (stockSource == null) {
				invalidRequest("stockmanagement.stockitem.preferredvendornoexist");
			}
			stockItem.setPreferredVendor(stockSource);
		} else {
			stockItem.setPreferredVendor(null);
		}
		
		if (!StringUtils.isBlank(stockItemDTO.getDispensingUnitUuid())) {
			Concept concept = Context.getConceptService().getConceptByUuid(stockItemDTO.getDispensingUnitUuid());
			if (concept != null) {
				stockItem.setDispensingUnit(concept);
			} else {
				invalidRequest("stockmanagement.stockitem.dispensingunitnoexist");
			}
		} else {
			stockItem.setDispensingUnit(null);
		}
		
		if (!StringUtils.isBlank(stockItemDTO.getCategoryUuid())) {
			Concept concept = Context.getConceptService().getConceptByUuid(stockItemDTO.getCategoryUuid());
			if (concept != null) {
				stockItem.setCategory(concept);
			} else {
				invalidRequest("stockmanagement.stockitem.categorynoexist");
			}
		} else {
			stockItem.setCategory(null);
		}
		
		if (!isNew) {
			
			if (!StringUtils.isBlank(stockItemDTO.getPurchasePriceUoMUuid()) && stockItemDTO.getPurchasePrice() != null) {
				StockItemPackagingUOM stockItemPackagingUOM = service()
				        .getStockItemPackagingUOMByUuid(stockItemDTO.getPurchasePriceUoMUuid());
				if (stockItemPackagingUOM == null) {
					invalidRequest("stockmanagement.stockitem.purchasepriceuomnoexist");
				} else if (!stockItemPackagingUOM.getStockItem().getId().equals(stockItem.getId())) {
					invalidRequest("stockmanagement.stockitem.purchasepriceuomnotrelatedtostockitem");
				}
				stockItem.setPurchasePriceUoM(stockItemPackagingUOM);
				stockItem.setPurchasePrice(stockItemDTO.getPurchasePrice());
			} else {
				stockItem.setPurchasePrice(null);
				stockItem.setPurchasePriceUoM(null);
			}
			
			if (!StringUtils.isBlank(stockItemDTO.getReorderLevelUoMUuid()) && stockItemDTO.getReorderLevel() != null) {
				StockItemPackagingUOM stockItemPackagingUOM = service()
				        .getStockItemPackagingUOMByUuid(stockItemDTO.getReorderLevelUoMUuid());
				if (stockItemPackagingUOM == null) {
					invalidRequest("stockmanagement.stockitem.reorderleveluomnoexist");
				} else if (!stockItemPackagingUOM.getStockItem().getId().equals(stockItem.getId())) {
					invalidRequest("stockmanagement.stockitem.reorderleveluomnotrelatedtostockitem");
				}
				stockItem.setReorderLevelUOM(stockItemPackagingUOM);
				stockItem.setReorderLevel(stockItemDTO.getReorderLevel());
			} else {
				stockItem.setReorderLevel(null);
				stockItem.setReorderLevelUOM(null);
			}
			
			if (!StringUtils.isBlank(stockItemDTO.getDispensingUnitPackagingUoMUuid())) {
				StockItemPackagingUOM stockItemPackagingUOM = service()
				        .getStockItemPackagingUOMByUuid(stockItemDTO.getDispensingUnitPackagingUoMUuid());
				if (stockItemPackagingUOM == null) {
					invalidRequest("stockmanagement.stockitem.dispensingunituomnoexist");
				} else if (!stockItemPackagingUOM.getStockItem().getId().equals(stockItem.getId())) {
					invalidRequest("stockmanagement.stockitem.dispensingunituomnotrelatedtostockitem");
				}
				stockItem.setDispensingUnitPackagingUoM(stockItemPackagingUOM);
			} else {
				stockItem.setDispensingUnitPackagingUoM(null);
			}
			
			if (!StringUtils.isBlank(stockItemDTO.getDefaultStockOperationsUoMUuid())) {
				StockItemPackagingUOM stockItemPackagingUOM = service()
				        .getStockItemPackagingUOMByUuid(stockItemDTO.getDefaultStockOperationsUoMUuid());
				if (stockItemPackagingUOM == null) {
					throw new StockManagementException("stockmanagement.stockitem.defaultstockoperationsuomnoexist");
				} else if (!stockItemPackagingUOM.getStockItem().getId().equals(stockItem.getId())) {
					invalidRequest("stockmanagement.stockitem.defaultstockoperationsuomnotrelatedtostockitem");
				}
				stockItem.setDefaultStockOperationsUoM(stockItemPackagingUOM);
			} else {
				stockItem.setDefaultStockOperationsUoM(null);
			}
		}
		
		service().saveStockItem(stockItem);
		
		return stockItem;
	}
	
	public StockItemPackagingUOM saveStockItemPackagingUOM(StockItemPackagingUOMDTO stockItemPackagingUOMDTO) {
		Context.requirePrivilege(Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE);
		return writeTransaction().execute(status -> savePackaging(stockItemPackagingUOMDTO));
	}
	
	private StockItemPackagingUOM savePackaging(StockItemPackagingUOMDTO stockItemPackagingUOMDTO) {
		StockItemPackagingUOM stockItemPackagingUOM = null;
		if (stockItemPackagingUOMDTO.getUuid() != null) {
			stockItemPackagingUOM = service().getStockItemPackagingUOMByUuid(stockItemPackagingUOMDTO.getUuid());
			if (stockItemPackagingUOM == null) {
				invalidRequest("stockmanagement.stockitempackagingunit.notexists");
			}
			stockItemPackagingUOM.setChangedBy(Context.getAuthenticatedUser());
			stockItemPackagingUOM.setDateChanged(new Date());
		} else {
			stockItemPackagingUOM = new StockItemPackagingUOM();
			stockItemPackagingUOM.setCreator(Context.getAuthenticatedUser());
			stockItemPackagingUOM.setDateCreated(new Date());
			StockItem stockItem = service().getStockItemByUuid(stockItemPackagingUOMDTO.getStockItemUuid());
			if (stockItem == null) {
				invalidRequest("stockmanagement.stockitem.notexists");
			}
			stockItemPackagingUOM.setStockItem(stockItem);
		}
		
		Concept concept = Context.getConceptService().getConceptByUuid(stockItemPackagingUOMDTO.getPackagingUomUuid());
		if (concept == null) {
			invalidRequest("stockmanagement.stockitempackagingunit.packaginguomnotexist");
		}
		stockItemPackagingUOM.setPackagingUom(concept);
		stockItemPackagingUOM.setFactor(stockItemPackagingUOMDTO.getFactor());
		
		return service().saveStockItemPackagingUOM(stockItemPackagingUOM);
		
	}
}
