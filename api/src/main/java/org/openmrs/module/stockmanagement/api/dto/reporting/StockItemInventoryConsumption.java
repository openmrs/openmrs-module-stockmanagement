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

import org.openmrs.module.stockmanagement.api.dto.StockItemInventory;

import java.math.BigDecimal;

public class StockItemInventoryConsumption extends StockItemInventory {
	
	private BigDecimal closingQuantity;
	
	private BigDecimal quantityConsumed;
	
	private BigDecimal quantityReceived;
	
	private BigDecimal consumptionRate;
	
	public BigDecimal getQuantityConsumed() {
		return quantityConsumed;
	}
	
	public void setQuantityConsumed(BigDecimal quantityConsumed) {
		this.quantityConsumed = quantityConsumed;
	}
	
	public BigDecimal getClosingQuantity() {
		return closingQuantity;
	}
	
	public void setClosingQuantity(BigDecimal closingQuantity) {
		this.closingQuantity = closingQuantity;
	}
	
	public BigDecimal getQuantityReceived() {
		return quantityReceived;
	}
	
	public void setQuantityReceived(BigDecimal quantityReceived) {
		this.quantityReceived = quantityReceived;
	}
	
	public BigDecimal getConsumptionRate() {
		return consumptionRate;
	}
	
	public void setConsumptionRate(BigDecimal consumptionRate) {
		this.consumptionRate = consumptionRate;
	}
}
