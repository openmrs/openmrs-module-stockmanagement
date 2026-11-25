/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.reporting.impl;

import org.apache.commons.lang.time.DateUtils;
import org.openmrs.module.stockmanagement.api.ModuleConstants;
import org.openmrs.module.stockmanagement.api.dto.StockInventoryResult;
import org.openmrs.module.stockmanagement.api.dto.StockItemInventory;
import org.openmrs.module.stockmanagement.api.dto.StockItemInventorySearchFilter;
import org.openmrs.module.stockmanagement.api.dto.reporting.StockItemInventoryConsumption;
import org.openmrs.module.stockmanagement.api.reporting.ReportGenerator;
import org.openmrs.module.stockmanagement.api.utils.DateUtil;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class StockConsumptionReport extends StockItemInventoryReport<StockItemInventoryConsumption> {
	
	@Override
	protected void preWriteBuffer(StockItemInventorySearchFilter inventorySearchFilter,
	        StockInventoryResult stockInventoryResult) {
		if (!stockInventoryResult.getData().isEmpty()) {
			stockManagementService.setStockItemInformation(stockInventoryResult.getData());
			if (!stockInventoryResult.getData().isEmpty()) {
				stockManagementService.postProcessInventoryResult(inventorySearchFilter, stockInventoryResult);
				BigDecimal daysInMonth = ModuleConstants.positiveValue(
				    ModuleConstants.REPORTING_CALCULATIONS_NO_DAYS_IN_MONTH,
				    ModuleConstants.getGlobalPropertyValue(ModuleConstants.REPORTING_CALCULATIONS_NO_DAYS_IN_MONTH,
				        BigDecimal.valueOf(30.5)),
				    BigDecimal.valueOf(30.5));
				stockInventoryResult.getData().forEach(p -> {
					StockItemInventoryConsumption consumption = ((StockItemInventoryConsumption) p);
					if (consumption.getQuantityFactor() != null) {
						if (consumption.getClosingQuantity() != null) {
							consumption.setClosingQuantity(consumption.getClosingQuantity()
							        .divide(consumption.getQuantityFactor(), 5, BigDecimal.ROUND_HALF_EVEN));
						}
						if (consumption.getQuantityConsumed() != null) {
							consumption.setQuantityConsumed(consumption.getQuantityConsumed()
							        .divide(consumption.getQuantityFactor(), 5, BigDecimal.ROUND_HALF_EVEN));
						}
						if (consumption.getQuantityReceived() != null) {
							consumption.setQuantityReceived(consumption.getQuantityReceived()
							        .divide(consumption.getQuantityFactor(), 5, BigDecimal.ROUND_HALF_EVEN));
						}
					}
					if (consumption.getQuantityConsumed() != null && inventorySearchFilter.getStartDate() != null
					        && inventorySearchFilter.getEndDate() != null) {
						long totalMiliseconds = Math.abs(
						    inventorySearchFilter.getEndDate().getTime() - inventorySearchFilter.getStartDate().getTime());
						long dayCount = Math.max(TimeUnit.DAYS.convert(totalMiliseconds, TimeUnit.MILLISECONDS), 1);
						consumption.setConsumptionRate(consumption.getQuantityConsumed().divide(
						    BigDecimal.valueOf(dayCount).divide(daysInMonth, 5, BigDecimal.ROUND_HALF_EVEN), 5,
						    BigDecimal.ROUND_HALF_EVEN));
					}
				});
			}
		}
	}
	
	@Override
	protected Class<StockItemInventoryConsumption> getStockItemInventoryClass() {
		return StockItemInventoryConsumption.class;
	}
	
	@Override
	protected boolean includeConsumptionInfo() {
		return true;
	}
	
	@Override
	protected BigDecimal getClosingQuantity(StockItemInventoryConsumption row) {
		return row.getClosingQuantity();
	}
	
	@Override
	protected BigDecimal getQuantityReceived(StockItemInventoryConsumption row) {
		return row.getQuantityReceived();
	}
	
	@Override
	protected BigDecimal getQuantityConsumed(StockItemInventoryConsumption row) {
		return row.getQuantityConsumed();
	}
	
	@Override
	protected BigDecimal getConsumptionRate(StockItemInventoryConsumption row) {
		return row.getConsumptionRate();
	}
	
	protected void setFilters(StockItemInventorySearchFilter filter, Properties parameters) {
		Date startDate = getStartDate(parameters);
		Date endDate = getEndDate(parameters);
		filter.setStartDate(startDate);
		if (endDate != null) {
			filter.setEndDate(DateUtil.endOfDay(endDate));
		}
		filter.setInventoryMode(StockItemInventorySearchFilter.InventoryMode.Consumption);
	}
}
