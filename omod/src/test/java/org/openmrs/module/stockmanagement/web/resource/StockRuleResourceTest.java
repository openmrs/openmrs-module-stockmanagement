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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.api.ValidationException;
import org.openmrs.module.stockmanagement.api.dto.StockRuleDTO;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;

public class StockRuleResourceTest extends BaseModuleWebContextSensitiveTest {
	
	private final StockRuleResource resource = new StockRuleResource();
	
	@BeforeEach
	public void setup() throws Exception {
		initializeInMemoryDatabase();
	}
	
	@Test
	public void save_shouldRejectAStockRuleWithoutAStockItemUuid() {
		StockRuleDTO dto = new StockRuleDTO();
		
		ValidationException exception = assertThrows(ValidationException.class, () -> resource.save(dto));
		
		assertNotNull(exception.getErrors().getFieldError("stockItemUuid"));
	}
}
