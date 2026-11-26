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

import org.openmrs.module.stockmanagement.api.dto.StockInventoryResult;
import org.openmrs.module.stockmanagement.api.dto.StockItemInventorySearchFilter;

import java.math.BigDecimal;

public class StockOutReport extends StockStatusReport {
	
	private BigDecimal oneHundered = BigDecimal.valueOf(100);
	
	@Override
	protected void preWriteBuffer(StockItemInventorySearchFilter inventorySearchFilter,
	        StockInventoryResult stockInventoryResult) {
		if (!stockInventoryResult.getData().isEmpty()) {
			stockInventoryService.setStockItemInformation(stockInventoryResult.getData());
			if (maxReorderLevelRatio == null) {
				maxReorderLevelRatio = BigDecimal.ZERO;
			}
			stockInventoryResult.getData().removeIf(p -> {
				return p.getQuantity()
				        .compareTo(maxReorderLevelRatio.divide(oneHundered, 5, BigDecimal.ROUND_HALF_EVEN)
				                .multiply(p.getReorderLevel() == null ? BigDecimal.ZERO
				                        : p.getReorderLevel().multiply(p.getReorderLevelFactor()))) > 0;
			});
			if (!stockInventoryResult.getData().isEmpty()) {
				stockInventoryService.postProcessInventoryResult(inventorySearchFilter, stockInventoryResult);
			}
		}
	}
}
