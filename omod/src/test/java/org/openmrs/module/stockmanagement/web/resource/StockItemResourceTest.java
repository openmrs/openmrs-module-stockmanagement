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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Drug;
import org.openmrs.api.ValidationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.openmrs.module.stockmanagement.api.dto.StockItemDTO;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;

public class StockItemResourceTest extends BaseModuleWebContextSensitiveTest {
	
	private StockItemResource resource;
	
	@BeforeEach
	public void setup() throws Exception {
		initializeInMemoryDatabase();
		resource = new StockItemResource();
	}
	
	@Test
	public void save_shouldRejectAReorderLevelWithoutAUnit() {
		StockItemDTO dto = newStockItem();
		dto.setReorderLevel(BigDecimal.ONE);
		
		ValidationException exception = assertThrows(ValidationException.class, () -> resource.save(dto));
		
		assertNotNull(exception.getErrors().getFieldError("reorderLevel"));
	}
	
	@Test
	public void save_shouldRejectANegativeReorderLevel() {
		StockItemDTO dto = newStockItem();
		dto.setReorderLevel(BigDecimal.ONE.negate());
		dto.setReorderLevelUoMUuid("some-packaging-uom-uuid");
		
		ValidationException exception = assertThrows(ValidationException.class, () -> resource.save(dto));
		
		assertNotNull(exception.getErrors().getFieldError("reorderLevel"));
	}
	
	@Test
	public void save_shouldReturnBadRequestForBusinessFailuresWithASafeMessage() throws Exception {
		StockItemRestMapper service = mock(StockItemRestMapper.class);
		resource.setItemMapper(service);
		for (String message : Arrays.asList("Invalid stock item", null, "", " ")) {
			doThrow(new StockManagementException(message)).when(service).saveStockItem(any(StockItemDTO.class));
			
			IllegalRequestException exception = assertThrows(IllegalRequestException.class,
			    () -> resource.save(newStockItem()));
			
			assertEquals(message == null || message.trim().isEmpty() ? "Unable to save stock item" : message,
			    exception.getMessage());
			assertNull(exception.getCause());
			exception.setStackTrace(new StackTraceElement[0]);
			HttpServletResponse response = mock(HttpServletResponse.class);
			new BaseRestController().handleException(exception, mock(HttpServletRequest.class), response);
			verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}
	
	@Test
	public void saveAndGet_shouldSaveAndRetrieveStockItem() {
		Drug drug = Context.getConceptService().getDrug(3);
		StockItemDTO dto = new StockItemDTO();
		dto.setDrugUuid(drug.getUuid());
		dto.setDispensingUnitUuid(Context.getConceptService().getConcept(20).getUuid());
		dto.setCommonName("Amoxicillin 250mg");
		dto.setHasExpiration(true);
		dto.setExpiryNotice(60);
		
		StockItemDTO saved = resource.save(dto);
		assertNotNull(saved);
		assertNotNull(saved.getUuid());
		assertEquals("Amoxicillin 250mg", saved.getCommonName());
		assertEquals(drug.getUuid(), saved.getDrugUuid());
		
		StockItemDTO retrieved = resource.getByUniqueId(saved.getUuid());
		assertNotNull(retrieved);
		assertEquals(saved.getUuid(), retrieved.getUuid());
		assertEquals("Amoxicillin 250mg", retrieved.getCommonName());
	}
	
	private StockItemDTO newStockItem() {
		StockItemDTO dto = new StockItemDTO();
		dto.setConceptUuid("some-concept-uuid");
		dto.setHasExpiration(false);
		return dto;
	}
}
