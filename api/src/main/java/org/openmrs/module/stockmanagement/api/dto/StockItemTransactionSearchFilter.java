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

import java.util.Date;

public class StockItemTransactionSearchFilter {
	
	public String uuid;
	
	public Integer stockOperationId;
	
	public Integer stockItemId;
	
	private Integer partyId;
	
	private Integer startIndex;
	
	private Integer limit;
	
	Date transactionDateMin;
	
	Date transactionDateMax;
	
	public Integer afterLastStockOperationId;
	
	private Boolean isPatientTransaction;
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public Integer getStockItemId() {
		return stockItemId;
	}
	
	public void setStockItemId(Integer stockItemId) {
		this.stockItemId = stockItemId;
	}
	
	public Integer getPartyId() {
		return partyId;
	}
	
	public void setPartyId(Integer partyId) {
		this.partyId = partyId;
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
	
	public Integer getStockOperationId() {
		return stockOperationId;
	}
	
	public void setStockOperationId(Integer stockOperationId) {
		this.stockOperationId = stockOperationId;
	}
	
	public Date getTransactionDateMin() {
		return transactionDateMin;
	}
	
	public void setTransactionDateMin(Date transactionDateMin) {
		this.transactionDateMin = transactionDateMin;
	}
	
	public Date getTransactionDateMax() {
		return transactionDateMax;
	}
	
	public void setTransactionDateMax(Date transactionDateMax) {
		this.transactionDateMax = transactionDateMax;
	}
	
	public Integer getAfterLastStockOperationId() {
		return afterLastStockOperationId;
	}
	
	public void setAfterLastStockOperationId(Integer afterLastStockOperationId) {
		this.afterLastStockOperationId = afterLastStockOperationId;
	}
	
	public Boolean getIsPatientTransaction() {
		return isPatientTransaction;
	}
	
	public void setIsPatientTransaction(Boolean isPatientTransaction) {
		this.isPatientTransaction = isPatientTransaction;
	}
	
}
