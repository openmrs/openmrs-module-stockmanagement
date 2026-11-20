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

import java.math.BigDecimal;

public class StockRuleCurrentQuantity {
	
	private Integer stockItemId;
	
	private Integer locationId;
	
	private BigDecimal quantity;
	
	private Boolean enableDescendants;
	
	public StockRuleCurrentQuantity() {
	}
	
	public StockRuleCurrentQuantity(Integer stockItemId, Integer locationId, BigDecimal quantity,
	    Boolean enableDescendants) {
		this.stockItemId = stockItemId;
		this.locationId = locationId;
		this.quantity = quantity;
		this.enableDescendants = enableDescendants;
	}
	
	public Integer getStockItemId() {
		return stockItemId;
	}
	
	public void setStockItemId(Integer stockItemId) {
		this.stockItemId = stockItemId;
	}
	
	public Integer getLocationId() {
		return locationId;
	}
	
	public void setLocationId(Integer locationId) {
		this.locationId = locationId;
	}
	
	public BigDecimal getQuantity() {
		return quantity;
	}
	
	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}
	
	public Boolean getEnableDescendants() {
		return enableDescendants;
	}
	
	public void setEnableDescendants(Boolean enableDescendants) {
		this.enableDescendants = enableDescendants;
	}
	
	public int getStockItemLocationEnableDescendantsHashCode() {
		int result = stockItemId.hashCode();
		result = 31 * result + locationId.hashCode();
		result = 31 * result + enableDescendants.hashCode();
		return result;
	}
}
