/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.openmrs.module.stockmanagement.api.dto.StockOperationAction;

public class StockOperationTypeTest {
	
	@Test
	public void getOperationType_shouldKeepTheCodesClientsAndExistingDataDependOn() {
		List<String> codes = Arrays.stream(StockOperationType.values()).map(StockOperationType::getOperationType)
		        .collect(Collectors.toList());
		
		assertEquals(Arrays.asList("adjustment", "disposed", "transferout", "receipt", "return", "stockissue", "requisition",
		    "stocktake", "initial"), codes);
	}
	
	@Test
	public void fromOperationType_shouldFindEveryTypeByItsCode() {
		for (StockOperationType type : StockOperationType.values()) {
			assertSame(type, StockOperationType.fromOperationType(type.getOperationType()));
		}
	}
	
	@Test
	public void fromOperationType_shouldReturnNullForAnUnknownOrMissingCode() {
		assertNull(StockOperationType.fromOperationType("erequisition"));
		assertNull(StockOperationType.fromOperationType(null));
	}
	
	@Test
	public void getAcronym_shouldKeepTheOperationNumberPrefixes() {
		List<String> acronyms = Arrays.stream(StockOperationType.values()).map(type -> type.getProcessor().getAcronym())
		        .collect(Collectors.toList());
		
		assertEquals(Arrays.asList("ADJ", "DIS", "TFO", "RCPT", "RTU", "SIS", "REQ", "STK", "IST"), acronyms);
	}
	
	@Test
	public void shouldDescribeWhereStockComesFromAndGoesTo() {
		assertEquals(LocationType.Other, StockOperationType.RECEIPT.getProcessor().getSourceType());
		assertEquals(LocationType.Location, StockOperationType.RECEIPT.getProcessor().getDestinationType());
		assertEquals(LocationType.Other, StockOperationType.TRANSFER_OUT.getProcessor().getDestinationType());
		assertEquals(LocationType.Location, StockOperationType.STOCK_ISSUE.getProcessor().getDestinationType());
		assertTrue(StockOperationType.ADJUSTMENT.getProcessor().getHasSource());
		assertFalse(StockOperationType.ADJUSTMENT.getProcessor().getHasDestination());
	}
	
	@Test
	public void notifiesOn_shouldOnlyNotifyOnProgressOfReceiptsAndTransfersOut() {
		assertTrue(StockOperationType.RECEIPT.getProcessor().notifiesOn(StockOperationAction.Action.SUBMIT));
		assertTrue(StockOperationType.TRANSFER_OUT.getProcessor().notifiesOn(StockOperationAction.Action.COMPLETE));
		assertFalse(StockOperationType.RECEIPT.getProcessor().notifiesOn(StockOperationAction.Action.CANCEL));
		assertFalse(StockOperationType.STOCK_ISSUE.getProcessor().notifiesOn(StockOperationAction.Action.SUBMIT));
	}
}
