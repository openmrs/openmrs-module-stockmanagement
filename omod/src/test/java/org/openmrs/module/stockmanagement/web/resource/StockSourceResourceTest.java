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
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.model.StockSource;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class StockSourceResourceTest {
	
	private StockManagementService service;
	
	private StockSourceResource resource;
	
	@BeforeEach
	public void setUp() {
		service = mock(StockManagementService.class);
		resource = new StockSourceResource() {
			
			@Override
			protected StockManagementService getStockManagementService() {
				return service;
			}
		};
	}
	
	@Test
	public void save_shouldReturnABadRequestWhenTheServiceRejectsADuplicate() {
		StockSource stockSource = new StockSource();
		when(service.saveStockSource(stockSource))
		        .thenThrow(new StockManagementException("A stock source named \"Watsons\" already exists"));
		
		IllegalRequestException exception = assertThrows(IllegalRequestException.class, () -> resource.save(stockSource));
		assertEquals("A stock source named \"Watsons\" already exists", exception.getMessage());
		
	}
	
	@Test
	public void save_shouldReturnTheSavedStockSource() {
		StockSource stockSource = new StockSource();
		when(service.saveStockSource(stockSource)).thenReturn(stockSource);
		
		assertSame(stockSource, resource.save(stockSource));
	}
}
