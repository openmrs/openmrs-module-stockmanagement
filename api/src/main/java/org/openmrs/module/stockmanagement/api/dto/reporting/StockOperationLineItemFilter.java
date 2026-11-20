/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.dto.reporting;

import org.openmrs.Location;
import org.openmrs.module.stockmanagement.api.model.StockOperationStatus;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;

import java.util.Date;
import java.util.List;

public class StockOperationLineItemFilter extends ReportFilter {
	
	private Integer destinationPartyId;
	
	private Boolean destinationPartyChildLocations;
	
	private Integer sourcePartyId;
	
	private Boolean sourcePartyChildLocations;
	
	private List<StockOperationType> stockOperationTypes;
	
	private List<StockOperationStatus> stockOperationStatuses;
	
	private Integer stockOperationIdMin;
	
	private Integer stockOperationItemIdMin;
	
	private boolean includeRequisitionInfo;
	
	public StockOperationLineItemFilter() {
		includeRequisitionInfo = false;
	}
	
	public Integer getDestinationPartyId() {
		return destinationPartyId;
	}
	
	public void setDestinationPartyId(Integer destinationPartyId) {
		this.destinationPartyId = destinationPartyId;
	}
	
	public Integer getSourcePartyId() {
		return sourcePartyId;
	}
	
	public void setSourcePartyId(Integer sourcePartyId) {
		this.sourcePartyId = sourcePartyId;
	}
	
	public List<StockOperationType> getStockOperationTypes() {
		return stockOperationTypes;
	}
	
	public void setStockOperationTypes(List<StockOperationType> stockOperationTypes) {
		this.stockOperationTypes = stockOperationTypes;
	}
	
	public List<StockOperationStatus> getStockOperationStatuses() {
		return stockOperationStatuses;
	}
	
	public void setStockOperationStatuses(List<StockOperationStatus> stockOperationStatuses) {
		this.stockOperationStatuses = stockOperationStatuses;
	}
	
	public Integer getStockOperationIdMin() {
		return stockOperationIdMin;
	}
	
	public void setStockOperationIdMin(Integer stockOperationIdMin) {
		this.stockOperationIdMin = stockOperationIdMin;
	}
	
	public Integer getStockOperationItemIdMin() {
		return stockOperationItemIdMin;
	}
	
	public void setStockOperationItemIdMin(Integer stockOperationItemIdMin) {
		this.stockOperationItemIdMin = stockOperationItemIdMin;
	}
	
	public Boolean getDestinationPartyChildLocations() {
		return destinationPartyChildLocations;
	}
	
	public void setDestinationPartyChildLocations(Boolean destinationPartyChildLocations) {
		this.destinationPartyChildLocations = destinationPartyChildLocations;
	}
	
	public Boolean getSourcePartyChildLocations() {
		return sourcePartyChildLocations;
	}
	
	public void setSourcePartyChildLocations(Boolean sourcePartyChildLocations) {
		this.sourcePartyChildLocations = sourcePartyChildLocations;
	}
	
	public boolean includeRequisitionInfo() {
		return includeRequisitionInfo;
	}
	
	public void setIncludeRequisitionInfo(boolean includeRequisitionInfo) {
		this.includeRequisitionInfo = includeRequisitionInfo;
	}
}
