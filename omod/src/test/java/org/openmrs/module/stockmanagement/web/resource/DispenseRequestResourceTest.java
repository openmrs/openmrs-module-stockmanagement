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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;

public class DispenseRequestResourceTest {
	
	private final DispenseRequestResource resource = new DispenseRequestResource();
	
	@Test
	public void covertToDispenseRequest_shouldThrowIllegalArgumentException_whenQuantityIsNull() {
		Map<String, Object> item = new HashMap<>();
		item.put("quantity", null);
		
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
		    () -> resource.covertToDispenseRequest(item));
		assertEquals("quantity can not be null", exception.getMessage());
	}
	
	@Test
	public void covertToDispenseRequest_shouldThrowIllegalArgumentException_whenQuantityIsOmitted() {
		Map<String, Object> item = new HashMap<>();
		
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
		    () -> resource.covertToDispenseRequest(item));
		assertEquals("quantity can not be null", exception.getMessage());
	}
	
	@Test
	public void covertToDispenseRequest_shouldThrowIllegalArgumentException_whenQuantityIsEmptyString() {
		Map<String, Object> item = new HashMap<>();
		item.put("quantity", "");
		
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
		    () -> resource.covertToDispenseRequest(item));
		assertEquals("quantity can not be null", exception.getMessage());
	}
	
	@Test
	public void covertToDispenseRequest_shouldPassQuantityCheck_whenQuantityIsValid() {
		Map<String, Object> item = new HashMap<>();
		item.put("quantity", "5");
		
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
		    () -> resource.covertToDispenseRequest(item));
		assertEquals("patient,order,and encounter can not be null", exception.getMessage());
	}
	
	@Test
	public void create_shouldNotThrowNullPointerException_whenDispenseItemsIsNull() {
		SimpleObject propertiesToCreate = new SimpleObject();
		propertiesToCreate.put("dispenseItems", null);
		
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
		    () -> resource.create(propertiesToCreate, mock(RequestContext.class)));
		assertEquals("quantity can not be null", exception.getMessage());
	}
}
