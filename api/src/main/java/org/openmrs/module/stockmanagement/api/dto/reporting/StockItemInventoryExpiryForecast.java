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
import java.util.Date;
import java.util.List;

public class StockItemInventoryExpiryForecast extends StockItemInventoryForecast {
	
	private BigDecimal loss;
	
	private BigDecimal stockItemConsumptionRate;
	
	private List<BigDecimal> forecastBalances;
	
	public BigDecimal getLoss() {
		return loss;
	}
	
	public void setLoss(BigDecimal loss) {
		this.loss = loss;
	}
	
	public BigDecimal getStockItemConsumptionRate() {
		return stockItemConsumptionRate;
	}
	
	public void setStockItemConsumptionRate(BigDecimal stockItemConsumptionRate) {
		this.stockItemConsumptionRate = stockItemConsumptionRate;
	}
	
	public List<BigDecimal> getForecastBalances() {
		return forecastBalances;
	}
	
	public void setForecastBalances(List<BigDecimal> forecastBalances) {
		this.forecastBalances = forecastBalances;
	}
}
