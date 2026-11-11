/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.stockmanagement.api.model.StockBatch;
import org.openmrs.module.stockmanagement.api.model.StockItem;

public class StockBatchMatchingTest {
	
	private static final Date JUNE = new Date(1813968000000L);
	
	private static final Date DECEMBER = new Date(1829779200000L);
	
	private StockItem expiringItem;
	
	private StockItem nonExpiringItem;
	
	@Before
	public void setUp() {
		expiringItem = new StockItem();
		expiringItem.setId(1);
		expiringItem.setHasExpiration(true);
		nonExpiringItem = new StockItem();
		nonExpiringItem.setId(2);
		nonExpiringItem.setHasExpiration(false);
	}
	
	private StockBatch batch(StockItem stockItem, String batchNo, Date expiration) {
		StockBatch stockBatch = new StockBatch();
		stockBatch.setStockItem(stockItem);
		stockBatch.setBatchNo(batchNo);
		stockBatch.setExpiration(expiration);
		return stockBatch;
	}
	
	@Test
	public void isSameStockBatch_shouldMatchTheSameBatchNumberAndExpiry() {
		assertTrue(StockManagementServiceImpl.isSameStockBatch(batch(expiringItem, "B-01", JUNE), expiringItem, "b-01",
		    new Date(JUNE.getTime())));
	}
	
	@Test
	public void isSameStockBatch_shouldNotMatchTheSameBatchNumberWithADifferentExpiry() {
		assertFalse(StockManagementServiceImpl.isSameStockBatch(batch(expiringItem, "B-01", JUNE), expiringItem, "B-01",
		    DECEMBER));
	}
	
	@Test
	public void isSameStockBatch_shouldNotFailWhenTheCandidateHasNoExpiry() {
		assertFalse(StockManagementServiceImpl.isSameStockBatch(batch(expiringItem, "B-01", null), expiringItem, "B-01",
		    JUNE));
	}
	
	@Test
	public void isSameStockBatch_shouldNotMatchADifferentBatchNumberOrStockItem() {
		assertFalse(StockManagementServiceImpl.isSameStockBatch(batch(expiringItem, "B-01", JUNE), expiringItem, "B-02",
		    JUNE));
		assertFalse(StockManagementServiceImpl.isSameStockBatch(batch(nonExpiringItem, "B-01", null), expiringItem,
		    "B-01", JUNE));
	}
	
	@Test
	public void isSameStockBatch_shouldMatchOnBatchNumberAloneForItemsThatDoNotExpire() {
		assertTrue(StockManagementServiceImpl.isSameStockBatch(batch(nonExpiringItem, "B-01", null), nonExpiringItem,
		    "B-01", JUNE));
		assertFalse(StockManagementServiceImpl.isSameStockBatch(batch(nonExpiringItem, "B-01", JUNE), nonExpiringItem,
		    "B-01", JUNE));
	}
}
