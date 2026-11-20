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

public class StockBatchSearchFilter {
	
	private Integer stockItemId;
	
	private String stockItemUuid;
	
	private String stockBatchUuid;
	
	private List<Integer> stockBatchIds;
	
	private Boolean excludeExpired;
	
	private boolean includeVoided;
	
	private Integer startIndex;
	
	private Integer limit;
	
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
	
	public boolean getIncludeVoided() {
		return includeVoided;
	}
	
	public void setIncludeVoided(boolean includeVoided) {
		this.includeVoided = includeVoided;
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
