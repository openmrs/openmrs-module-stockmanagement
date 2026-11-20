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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.StockOperationDTO;
import org.openmrs.module.webservices.rest.web.response.GenericRestException;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;

/**
 * Unit tests for the error translation in {@link StockOperationResource#save(StockOperationDTO)}:
 * business exceptions must never surface with a null message, unexpected exceptions must never leak
 * their details to the client, and the original exception is kept in the log rather than attached
 * as the cause (wrapErrorResponse appends each cause's message to the response).
 */
public class StockOperationResourceTest {
	
	private StockOperationResource resource;
	
	private StockManagementService service;
	
	@BeforeEach
	public void setup() {
		// bypass validateStockOperation: it needs a live OpenMRS context (covered by the
		// StockOperationDTOValidatorTest integration tests in the api module)
		resource = new StockOperationResource() {
			
			@Override
			protected void validateStockOperation(StockOperationDTO delegate) {
				// no-op
			}
		};
		service = mock(StockManagementService.class);
		resource.setStockManagementService(service);
	}
	
	@Test
	public void save_shouldReturnFallbackMessage_whenBusinessExceptionHasNullMessage() {
		StockManagementException original = new StockManagementException((String) null);
		when(service.saveStockOperation(any(StockOperationDTO.class))).thenThrow(original);
		
		IllegalRequestException exception = assertThrows(IllegalRequestException.class,
		    () -> resource.save(new StockOperationDTO()));
		assertNotNull(exception.getMessage(), "REST errors must never carry a null message");
		assertEquals("Unable to save stock operation", exception.getMessage());
		// wrapErrorResponse appends each cause's message to the response: the cause must stay out
		assertNull(exception.getCause());
	}
	
	@Test
	public void save_shouldPreserveBusinessExceptionMessage() {
		when(service.saveStockOperation(any(StockOperationDTO.class)))
		        .thenThrow(new StockManagementException("Stock operation requires a destination"));
		
		IllegalRequestException exception = assertThrows(IllegalRequestException.class,
		    () -> resource.save(new StockOperationDTO()));
		assertEquals("Stock operation requires a destination", exception.getMessage());
		assertNull(exception.getCause());
	}
	
	@Test
	public void save_shouldReturnSafeGenericError_whenUnexpectedExceptionOccurs() {
		when(service.saveStockOperation(any(StockOperationDTO.class))).thenThrow(new NullPointerException());
		
		GenericRestException exception = assertThrows(GenericRestException.class,
		    () -> resource.save(new StockOperationDTO()));
		// unexpected failures must expose a safe generic message only
		assertEquals("Unable to save stock operation", exception.getMessage());
		assertNull(exception.getCause());
	}
}
