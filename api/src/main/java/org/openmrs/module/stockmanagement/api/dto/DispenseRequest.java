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

import org.openmrs.module.stockmanagement.api.model.StockItem;

import java.io.Serializable;
import java.math.BigDecimal;

public class DispenseRequest implements Serializable {
	
	private String locationUuid;
	
	private Integer patientId;
	
	private Integer orderId;
	
	private Integer encounterId;
	
	private String stockItemUuid;
	
	private String stockBatchUuid;
	
	private BigDecimal quantity;
	
	private String stockItemPackagingUOMUuid;
	
	public String getLocationUuid() {
		return locationUuid;
	}
	
	public void setLocationUuid(String locationUuid) {
		this.locationUuid = locationUuid;
	}
	
	public Integer getPatientId() {
		return patientId;
	}
	
	public void setPatientId(Integer patientId) {
		this.patientId = patientId;
	}
	
	public Integer getOrderId() {
		return orderId;
	}
	
	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}
	
	public Integer getEncounterId() {
		return encounterId;
	}
	
	public void setEncounterId(Integer encounterId) {
		this.encounterId = encounterId;
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
	
	public BigDecimal getQuantity() {
		return quantity;
	}
	
	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}
	
	public String getStockItemPackagingUOMUuid() {
		return stockItemPackagingUOMUuid;
	}
	
	public void setStockItemPackagingUOMUuid(String stockItemPackagingUOMUuid) {
		this.stockItemPackagingUOMUuid = stockItemPackagingUOMUuid;
	}
}
