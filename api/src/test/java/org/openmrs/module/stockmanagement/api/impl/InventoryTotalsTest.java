/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.openmrs.module.stockmanagement.api.dto.StockItemInventory;

public class InventoryTotalsTest {
	
	private static StockItemInventory row(int partyId, int stockItemId, String quantity) {
		StockItemInventory row = new StockItemInventory();
		row.setPartyId(partyId);
		row.setStockItemId(stockItemId);
		row.setLocationUuid("location-" + partyId);
		row.setQuantity(new BigDecimal(quantity));
		return row;
	}
	
	@Test
	public void totalByLocationStockItem_shouldKeepTheOrderInWhichEachItemFirstAppears() {
		List<StockItemInventory> rows = new ArrayList<>();
		for (int stockItemId = 1; stockItemId <= 10; stockItemId++) {
			rows.add(row(3, stockItemId, "1"));
		}
		rows.add(row(3, 2, "4"));
		rows.add(row(3, 1, "5"));
		
		List<StockItemInventory> totals = StockManagementServiceImpl.totalByLocationStockItem(rows);
		
		assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
		    totals.stream().map(StockItemInventory::getStockItemId).collect(Collectors.toList()));
		assertEquals(new BigDecimal("6"), totals.get(0).getQuantity());
		assertEquals(new BigDecimal("5"), totals.get(1).getQuantity());
		assertEquals(new BigDecimal("1"), totals.get(2).getQuantity());
		assertEquals("location-3", totals.get(0).getLocationUuid());
	}
	
	@Test
	public void totalByLocationStockItem_shouldKeepTheSameItemAtDifferentLocationsApart() {
		List<StockItemInventory> totals = StockManagementServiceImpl
		        .totalByLocationStockItem(Arrays.asList(row(4, 7, "2"), row(3, 7, "1"), row(4, 7, "3")));
		
		assertEquals(Arrays.asList(4, 3), totals.stream().map(StockItemInventory::getPartyId).collect(Collectors.toList()));
		assertEquals(new BigDecimal("5"), totals.get(0).getQuantity());
		assertEquals(new BigDecimal("1"), totals.get(1).getQuantity());
	}
}
