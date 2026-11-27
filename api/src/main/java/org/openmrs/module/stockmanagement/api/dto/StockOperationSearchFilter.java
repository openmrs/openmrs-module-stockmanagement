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

import org.openmrs.module.stockmanagement.api.model.StockOperationType;
import org.openmrs.module.stockmanagement.api.model.StockOperationStatus;

import java.util.Date;
import java.util.List;

public class StockOperationSearchFilter {
	
	private String stockOperationUuid;
	
	private List<StockOperationStatus> status;
	
	private List<StockOperationType> operationTypes;
	
	private Integer locationId;
	
	private Integer partyId;
	
	private Integer stockSourceId;
	
	private Boolean isLocationOther;
	
	private List<Integer> sourceTypeIds;
	
	private Integer stockItemId;
	
	private Date operationDateMin;
	
	private Date operationDateMax;
	
	private String operationNumber;
	
	private Integer startIndex;
	
	private String searchText;
	
	private Integer limit;
	
	private boolean includeVoided;
	
	public String getSearchText() {
		return searchText;
	}
	
	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}
	
	public Integer getStockSourceId() {
		return stockSourceId;
	}
	
	public void setStockSourceId(Integer stockSourceId) {
		this.stockSourceId = stockSourceId;
	}
	
	public Integer getPartyId() {
		return partyId;
	}
	
	public void setPartyId(Integer partyId) {
		this.partyId = partyId;
	}
	
	public boolean getIncludeVoided() {
		return includeVoided;
	}
	
	public void setIncludeVoided(boolean includeVoided) {
		this.includeVoided = includeVoided;
	}
	
	public String getStockOperationUuid() {
		return stockOperationUuid;
	}
	
	public void setStockOperationUuid(String stockOperationUuid) {
		this.stockOperationUuid = stockOperationUuid;
	}
	
	public List<StockOperationStatus> getStatus() {
		return status;
	}
	
	public void setStatus(List<StockOperationStatus> status) {
		this.status = status;
	}
	
	public List<StockOperationType> getOperationTypes() {
		return operationTypes;
	}
	
	public void setOperationTypes(List<StockOperationType> operationTypes) {
		this.operationTypes = operationTypes;
	}
	
	public Integer getLocationId() {
		return locationId;
	}
	
	public void setLocationId(Integer locationId) {
		this.locationId = locationId;
	}
	
	public Integer getStockItemId() {
		return stockItemId;
	}
	
	public void setStockItemId(Integer stockItemId) {
		this.stockItemId = stockItemId;
	}
	
	public Date getOperationDateMin() {
		return operationDateMin;
	}
	
	public void setOperationDateMin(Date operationDateMin) {
		this.operationDateMin = operationDateMin;
	}
	
	public Date getOperationDateMax() {
		return operationDateMax;
	}
	
	public void setOperationDateMax(Date operationDateMax) {
		this.operationDateMax = operationDateMax;
	}
	
	public String getOperationNumber() {
		return operationNumber;
	}
	
	public void setOperationNumber(String operationNumber) {
		this.operationNumber = operationNumber;
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
	
	public Boolean getIsLocationOther() {
		return isLocationOther;
	}
	
	public void setIsLocationOther(Boolean isLocationOther) {
		this.isLocationOther = isLocationOther;
	}
	
	public List<Integer> getSourceTypeIds() {
		return sourceTypeIds;
	}
	
	public void setSourceTypeIds(List<Integer> sourceTypeIds) {
		this.sourceTypeIds = sourceTypeIds;
	}
}
