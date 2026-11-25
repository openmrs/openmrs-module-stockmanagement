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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.api.ValidationException;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.PrivilegeScope;
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
	
	@Test
	public void getPermission_shouldUseTheRoleScopesOfTheCurrentRequest() {
		StockManagementService service = mock(StockManagementService.class);
		resource.setStockManagementService(service);
		// the first user can edit rules at location-a only, the next one at location-b
		when(service.getPrivilegeScopes(any(), any(), any(), anyList())).thenReturn(scopesAt("location-a"))
		        .thenReturn(scopesAt("location-b"));
		StockRuleDTO rule = new StockRuleDTO();
		rule.setLocationUuid("location-b");
		
		assertEquals(false, resource.getPermission(rule).get("canEdit"));
		assertEquals(true, resource.getPermission(rule).get("canEdit"));
	}
	
	private static HashSet<PrivilegeScope> scopesAt(String locationUuid) {
		PrivilegeScope scope = new PrivilegeScope();
		scope.setLocationUuid(locationUuid);
		return new HashSet<>(Collections.singletonList(scope));
	}
}
