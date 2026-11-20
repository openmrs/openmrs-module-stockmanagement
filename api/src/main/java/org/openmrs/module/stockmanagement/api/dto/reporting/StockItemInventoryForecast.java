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
import java.util.ArrayList;
import java.util.List;

public class StockItemInventoryForecast extends StockItemInventory {
	
	private List<BigDecimal> quantityConsumed;
	
	private BigDecimal consumptionRate;
	
	public StockItemInventoryForecast() {
		quantityConsumed = new ArrayList<>();
	}
	
	public List<BigDecimal> getQuantityConsumed() {
		return quantityConsumed;
	}
	
	public void setQuantityConsumed(List<BigDecimal> quantityConsumed) {
		this.quantityConsumed = quantityConsumed;
	}
	
	public BigDecimal getConsumptionRate() {
		return consumptionRate;
	}
	
	public void setConsumptionRate(BigDecimal consumptionRate) {
		this.consumptionRate = consumptionRate;
	}
}
