/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.dto;

import java.util.List;

public class StockOperationItemSearchFilter {
	
	private List<String> stockOperationUuids;
	
	private List<Integer> stockOperationIds;
	
	private String uuid;
	
	private Integer stockItemId;
	
	private String stockItemUuid;
	
	private Integer startIndex;
	
	private Integer limit;
	
	private boolean includeVoided;
	
	private boolean includeStockUnitName;
	
	private boolean includePackagingUnitName;
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public List<String> getStockOperationUuids() {
		return stockOperationUuids;
	}
	
	public void setStockOperationUuids(List<String> stockOperationUuid) {
		this.stockOperationUuids = stockOperationUuid;
	}
	
	public List<Integer> getStockOperationIds() {
		return stockOperationIds;
	}
	
	public void setStockOperationIds(List<Integer> stockOperationIds) {
		this.stockOperationIds = stockOperationIds;
	}
	
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
	
	public boolean getIncludeVoided() {
		return includeVoided;
	}
	
	public void setIncludeVoided(boolean includeVoided) {
		this.includeVoided = includeVoided;
	}
	
	public boolean getIncludePackagingUnitName() {
		return includePackagingUnitName;
	}
	
	public void setIncludePackagingUnitName(boolean includePackagingUnitName) {
		this.includePackagingUnitName = includePackagingUnitName;
	}
	
	public boolean getIncludeStockUnitName() {
		return includeStockUnitName;
	}
	
	public void setIncludeStockUnitName(boolean includeStockUnitName) {
		this.includeStockUnitName = includeStockUnitName;
	}
}
