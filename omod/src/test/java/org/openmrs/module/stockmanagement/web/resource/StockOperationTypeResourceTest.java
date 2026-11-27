/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.web.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.model.LocationType;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;

public class StockOperationTypeResourceTest extends BaseModuleWebContextSensitiveTest {
	
	@Test
	public void asRepresentation_shouldListEachConfiguredLocationTagWithItsSide() throws Exception {
		Context.getAdministrationService().setGlobalProperty("stockmanagement.stockissue.sourceLocationTags", "Main Store");
		Context.getAdministrationService().setGlobalProperty("stockmanagement.stockissue.destinationLocationTags",
		    "Main Pharmacy");
		
		SimpleObject type = new StockOperationTypeResource().asRepresentation(StockOperationType.STOCK_ISSUE,
		    Representation.DEFAULT);
		
		assertEquals("stockissue", type.get("operationType"));
		List<Map<String, Object>> scopes = type.get("stockOperationTypeLocationScopes");
		assertEquals(2, scopes.size());
		assertEquals("Main Store", scopes.get(0).get("locationTag"));
		assertEquals(true, scopes.get(0).get("isSource"));
		assertEquals(false, scopes.get(0).get("isDestination"));
		assertEquals("Main Pharmacy", scopes.get(1).get("locationTag"));
		assertEquals(false, scopes.get(1).get("isSource"));
		assertEquals(true, scopes.get(1).get("isDestination"));
	}
	
	@Test
	public void asRepresentation_shouldReturnNoLocationScopesWhenNoTagsAreConfigured() throws Exception {
		SimpleObject type = new StockOperationTypeResource().asRepresentation(StockOperationType.STOCK_ISSUE,
		    Representation.DEFAULT);
		
		List<Map<String, Object>> scopes = type.get("stockOperationTypeLocationScopes");
		assertTrue(scopes.isEmpty());
	}
	
	@Test
	public void asRepresentation_shouldReturnTheFlagsOfTheOperationType() throws Exception {
		SimpleObject type = new StockOperationTypeResource().asRepresentation(StockOperationType.RECEIPT,
		    Representation.DEFAULT);
		
		assertEquals("receipt", type.get("operationType"));
		assertEquals(true, type.get("hasSource"));
		assertEquals(LocationType.Other, type.get("sourceType"));
		assertEquals(true, type.get("hasDestination"));
		assertEquals(LocationType.Location, type.get("destinationType"));
	}
}
