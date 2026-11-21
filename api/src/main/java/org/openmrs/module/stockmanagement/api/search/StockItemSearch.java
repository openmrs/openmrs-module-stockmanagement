/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.search;

import org.openmrs.Concept;
import org.openmrs.Drug;

import java.util.List;

public class StockItemSearch {
	
	private String uuid;
	
	private Integer startIndex;
	
	private Integer limit;
	
	private Boolean isDrug;
	
	private List<Drug> drugs;
	
	private List<Concept> concepts;
	
	private List<Integer> stockItemIds;
	
	private Integer drugId;
	
	private Integer conceptId;
	
	private boolean includeVoided;
	
	private boolean searchEitherDrugsOrConcepts = false;
	
	private List<Concept> categories;
	
	private Integer categoryId;
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public Integer getStartIndex() {
		return startIndex;
	}
	
	public void setStartIndex(Integer startIndex) {
		this.startIndex = startIndex;
	}
	
	public Integer getLimit() {
		return limit;
	}
	
	public void setLimit(Integer limit) {
		this.limit = limit;
	}
	
	public Boolean getIsDrug() {
		return isDrug;
	}
	
	public void setIsDrug(Boolean isDrug) {
		this.isDrug = isDrug;
	}
	
	public List<Drug> getDrugs() {
		return drugs;
	}
	
	public void setDrugs(List<Drug> drugs) {
		this.drugs = drugs;
	}
	
	public List<Concept> getConcepts() {
		return concepts;
	}
	
	public void setConcepts(List<Concept> concepts) {
		this.concepts = concepts;
	}
	
	public boolean getIncludeVoided() {
		return includeVoided;
	}
	
	public void setIncludeVoided(boolean includeVoided) {
		this.includeVoided = includeVoided;
	}
	
	public boolean getSearchEitherDrugsOrConcepts() {
		return searchEitherDrugsOrConcepts;
	}
	
	public void setSearchEitherDrugsOrConcepts(boolean searchEitherDrugsOrConcepts) {
		this.searchEitherDrugsOrConcepts = searchEitherDrugsOrConcepts;
	}
	
	public List<Integer> getStockItemIds() {
		return stockItemIds;
	}
	
	public void setStockItemIds(List<Integer> stockItemIds) {
		this.stockItemIds = stockItemIds;
	}
	
	public Integer getDrugId() {
		return drugId;
	}
	
	public void setDrugId(Integer drugId) {
		this.drugId = drugId;
	}
	
	public Integer getConceptId() {
		return conceptId;
	}
	
	public void setConceptId(Integer conceptId) {
		this.conceptId = conceptId;
	}
	
	public List<Concept> getCategories() {
		return categories;
	}
	
	public void setCategories(List<Concept> categories) {
		this.categories = categories;
	}
	
	public Integer getCategoryId() {
		return categoryId;
	}
	
	public void setCategoryId(Integer categoryId) {
		this.categoryId = categoryId;
	}
	
	public static class ItemGroupFilter {
		
		private Integer drugId;
		
		private Integer conceptId;
		
		private Boolean isDrug;
		
		public Integer getDrugId() {
			return drugId;
		}
		
		public void setDrugId(Integer drugId) {
			this.drugId = drugId;
		}
		
		public Integer getConceptId() {
			return conceptId;
		}
		
		public void setConceptId(Integer conceptId) {
			this.conceptId = conceptId;
		}
		
		public Boolean getIsDrug() {
			return isDrug;
		}
		
		public void setIsDrug(Boolean isDrug) {
			this.isDrug = isDrug;
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			
			ItemGroupFilter that = (ItemGroupFilter) o;
			
			if (drugId != null ? !drugId.equals(that.drugId) : that.drugId != null)
				return false;
			if (conceptId != null ? !conceptId.equals(that.conceptId) : that.conceptId != null)
				return false;
			return !(isDrug != null ? !isDrug.equals(that.isDrug) : that.isDrug != null);
			
		}
		
		@Override
		public int hashCode() {
			int result = drugId != null ? drugId.hashCode() : 0;
			result = 31 * result + (conceptId != null ? conceptId.hashCode() : 0);
			result = 31 * result + (isDrug != null ? isDrug.hashCode() : 0);
			return result;
		}
	}
	
	private List<String> stockItemUuids;
	
	private boolean includeDispensingUnit;
	
	public List<String> getStockItemUuids() {
		return stockItemUuids;
	}
	
	public void setStockItemUuids(List<String> stockItemUuids) {
		this.stockItemUuids = stockItemUuids;
	}
	
	public boolean isIncludeVoided() {
		return includeVoided;
	}
	
	public boolean includingDispensingUnit() {
		return includeDispensingUnit;
	}
	
	public void setIncludeDispensingUnit(boolean includeDispensingUnit) {
		this.includeDispensingUnit = includeDispensingUnit;
	}
	
	public static class PackagingGroup {
		
		private Integer stockItemId;
		
		private List<Integer> packagingUomIds;
		
		public PackagingGroup() {
		}
		
		public PackagingGroup(Integer stockItemId, List<Integer> packagingUomIds) {
			this.stockItemId = stockItemId;
			this.packagingUomIds = packagingUomIds;
		}
		
		public Integer getStockItemId() {
			return stockItemId;
		}
		
		public void setStockItemId(Integer stockItemId) {
			this.stockItemId = stockItemId;
		}
		
		public List<Integer> getPackagingUomIds() {
			return packagingUomIds;
		}
		
		public void setPackagingUomIds(List<Integer> packagingUomIds) {
			this.packagingUomIds = packagingUomIds;
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			
			PackagingGroup that = (PackagingGroup) o;
			
			if (stockItemId != null ? !stockItemId.equals(that.stockItemId) : that.stockItemId != null)
				return false;
			return !(packagingUomIds != null ? !packagingUomIds.equals(that.packagingUomIds) : that.packagingUomIds != null);
			
		}
		
		@Override
		public int hashCode() {
			int result = stockItemId != null ? stockItemId.hashCode() : 0;
			result = 31 * result + (packagingUomIds != null ? packagingUomIds.hashCode() : 0);
			return result;
		}
	}
	
	private Integer stockItemId;
	
	private String stockItemUuid;
	
	private String stockBatchUuid;
	
	private List<Integer> stockBatchIds;
	
	private Boolean excludeExpired;
	
	private String locationUuid;
	
	private Boolean excludeEmptyStock;
	
	public Integer getStockItemId() {
		return stockItemId;
	}
	
	public void setStockItemId(Integer stockItemId) {
		this.stockItemId = stockItemId;
	}
	
	public String getStockItemUuid() {
		return stockItemUuid;
	}
	
	public void setStockItemUuid(String stockItemUuid) {
		this.stockItemUuid = stockItemUuid;
	}
	
	public String getStockBatchUuid() {
		return stockBatchUuid;
	}
	
	public void setStockBatchUuid(String stockBatchUuid) {
		this.stockBatchUuid = stockBatchUuid;
	}
	
	public List<Integer> getStockBatchIds() {
		return stockBatchIds;
	}
	
	public void setStockBatchIds(List<Integer> stockBatchIds) {
		this.stockBatchIds = stockBatchIds;
	}
	
	public Boolean getExcludeExpired() {
		return excludeExpired;
	}
	
	public void setExcludeExpired(Boolean excludeExpired) {
		this.excludeExpired = excludeExpired;
	}
	
	public String getLocationUuid() {
		return locationUuid;
	}
	
	public void setLocationUuid(String locationUuid) {
		this.locationUuid = locationUuid;
	}
	
	public Boolean getExcludeEmptyStock() {
		return excludeEmptyStock;
	}
	
	public void setExcludeEmptyStock(Boolean excludeEmptyStock) {
		this.excludeEmptyStock = excludeEmptyStock;
	}
}
