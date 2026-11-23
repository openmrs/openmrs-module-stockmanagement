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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.EntityUtil;
import org.openmrs.module.stockmanagement.api.StockSourceService;
import org.openmrs.module.stockmanagement.api.model.StockSource;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StockSourceResourceTest extends BaseModuleWebContextSensitiveTest {
	
	private StockSourceResource resource;
	
	private StockSourceService stockSourceService;
	
	@BeforeEach
	public void setup() throws Exception {
		initializeInMemoryDatabase();
		executeDataSet(EntityUtil.STOCK_OPERATION_TYPE_DATA_SET);
		stockSourceService = Context.getService(StockSourceService.class);
		resource = new StockSourceResource();
		resource.setStockSourceService(stockSourceService);
	}
	
	@Test
	public void getByUniqueId_shouldReturnStockSource() {
		StockSource stockSource = new StockSource();
		stockSource.setName("Direct Medical");
		stockSource.setAcronym("DM");
		stockSource.setSourceType(Context.getConceptService().getConcept(3));
		StockSource saved = stockSourceService.saveStockSource(stockSource);
		
		StockSource result = resource.getByUniqueId(saved.getUuid());
		assertNotNull(result);
		assertEquals("Direct Medical", result.getName());
		assertEquals("DM", result.getAcronym());
	}
	
	@Test
	public void save_shouldSaveStockSourceThroughResource() {
		StockSource stockSource = new StockSource();
		stockSource.setName("Resource Vendor");
		stockSource.setAcronym("RV");
		stockSource.setSourceType(Context.getConceptService().getConcept(3));
		
		StockSource saved = resource.save(stockSource);
		assertNotNull(saved);
		assertNotNull(saved.getId());
		
		StockSource fetched = stockSourceService.getStockSourceByUuid(saved.getUuid());
		assertNotNull(fetched);
		assertEquals("Resource Vendor", fetched.getName());
	}
	
	@Test
	public void doSearch_shouldReturnOnlyMatchingStockSources() {
		StockSource match = saveStockSource("Global Pharmaceuticals", "GP");
		StockSource other = saveStockSource("Other Vendor", "OV");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("q", "Global");
		RequestContext context = new RequestContext();
		context.setRequest(request);
		List<StockSource> results = ((AlreadyPaged<StockSource>) resource.doSearch(context)).getPageOfResults();
		assertTrue(results.contains(match));
		assertFalse(results.contains(other));
	}
	
	@Test
	public void doGetAll_shouldReturnEveryStockSourceWhateverTheLimit() {
		StockSource first = saveStockSource("Vendor A", "VA");
		StockSource second = saveStockSource("Vendor B", "VB");
		RequestContext context = new RequestContext();
		context.setRequest(new MockHttpServletRequest());
		context.setLimit(1);
		List<StockSource> results = ((AlreadyPaged<StockSource>) resource.doGetAll(context)).getPageOfResults();
		assertTrue(results.contains(first));
		assertTrue(results.contains(second));
	}
	
	@Test
	public void delete_shouldVoidStockSource() {
		StockSource stockSource = new StockSource();
		stockSource.setName("Vendor To Delete");
		stockSource.setAcronym("VTD");
		stockSource.setSourceType(Context.getConceptService().getConcept(3));
		StockSource saved = stockSourceService.saveStockSource(stockSource);
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContext context = new RequestContext();
		context.setRequest(request);
		
		resource.delete(saved, "Retiring test vendor", context);
		
		Context.flushSession();
		Context.clearSession();
		
		StockSource voided = stockSourceService.getStockSourceByUuid(saved.getUuid());
		assertTrue(voided.getVoided());
		assertEquals("Retiring test vendor", voided.getVoidReason());
	}
	
	private StockSource saveStockSource(String name, String acronym) {
		StockSource stockSource = new StockSource();
		stockSource.setName(name);
		stockSource.setAcronym(acronym);
		stockSource.setSourceType(Context.getConceptService().getConcept(3));
		return stockSourceService.saveStockSource(stockSource);
	}
}
