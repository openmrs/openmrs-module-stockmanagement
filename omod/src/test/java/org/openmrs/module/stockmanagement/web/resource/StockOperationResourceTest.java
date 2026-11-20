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
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
import org.openmrs.module.stockmanagement.api.model.StockOperationStatus;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;
import org.openmrs.module.webservices.rest.SimpleObject;
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
		StockOperationType operationType = new StockOperationType();
		operationType.setOperationType(StockOperationType.STOCK_ISSUE);
		operationType.setAllowBatchInfoUpdate(false);
		when(service.getStockOperationTypeByUuid(any())).thenReturn(operationType);
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
	
	@Test
	public void getPermission_shouldReflectTheCurrentStatusOfTheOperation() {
		SimpleObject dispatched = resource.getPermission(operation(StockOperationStatus.DISPATCHED));
		assertEquals(true, dispatched.get("canDisplayReceivedItems"));
		
		SimpleObject completed = resource.getPermission(operation(StockOperationStatus.COMPLETED));
		assertEquals(false, completed.get("canDisplayReceivedItems"));
	}
	
	@Test
	public void getPermission_shouldBeComputedAgainForEachRequest() {
		StockOperationDTO operation = operation(StockOperationStatus.DISPATCHED);
		
		assertNotSame(resource.getPermission(operation), resource.getPermission(operation));
	}
	
	// A locked operation that isn't SUBMITTED can be neither edited nor approved, so
	// getPermission doesn't look up the authenticated user's role scopes for it.
	private static StockOperationDTO operation(StockOperationStatus status) {
		StockOperationDTO operation = new StockOperationDTO();
		operation.setUuid("operation-uuid");
		operation.setOperationTypeUuid("operation-type-uuid");
		operation.setOperationType(StockOperationType.STOCK_ISSUE);
		operation.setStatus(status);
		operation.setLocked(true);
		operation.setVoided(false);
		return operation;
	}
}
