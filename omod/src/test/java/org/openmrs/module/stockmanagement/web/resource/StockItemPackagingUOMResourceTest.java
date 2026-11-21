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

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.api.ValidationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.StockItemService;
import org.openmrs.module.stockmanagement.api.dto.StockItemPackagingUOMDTO;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;

/**
 * Proves that {@link StockItemPackagingUOMResource#save} runs the StockItemPackagingUOMValidator
 * explicitly: webservices.rest 3.x no longer invokes it implicitly, and without it a packaging unit
 * with a zero or negative factor saved with a 201.
 */
public class StockItemPackagingUOMResourceTest extends BaseModuleWebContextSensitiveTest {
	
	private final StockItemPackagingUOMResource resource = new StockItemPackagingUOMResource();
	
	@BeforeEach
	public void setup() throws Exception {
		initializeInMemoryDatabase();
	}
	
	@Test
	public void save_shouldRejectAPackagingUomWithANonPositiveFactor() {
		StockItemPackagingUOMDTO dto = new StockItemPackagingUOMDTO();
		dto.setPackagingUomUuid("some-packaging-uom-uuid");
		dto.setFactor(BigDecimal.ZERO);
		
		assertThrows(ValidationException.class, () -> resource.save(dto));
	}
	
	@Test
	public void saveAndGet_shouldSaveAndRetrievePackagingUom() {
		StockItem item = new StockItem();
		item.setConcept(Context.getConceptService().getConcept(3));
		item.setCommonName("Item for UOM test");
		item.setHasExpiration(false);
		item.setIsDrug(false);
		Context.getService(StockItemService.class).saveStockItem(item);
		
		StockItemPackagingUOMDTO dto = new StockItemPackagingUOMDTO();
		dto.setStockItemUuid(item.getUuid());
		dto.setPackagingUomUuid(Context.getConceptService().getConcept(3).getUuid());
		dto.setFactor(BigDecimal.valueOf(10));
		
		StockItemPackagingUOMDTO saved = resource.save(dto);
		assertNotNull(saved);
		assertNotNull(saved.getUuid());
		assertEquals(item.getUuid(), saved.getStockItemUuid());
		assertEquals(0, BigDecimal.valueOf(10).compareTo(saved.getFactor()));
		
		StockItemPackagingUOMDTO fetched = resource.getByUniqueId(saved.getUuid());
		assertNotNull(fetched);
		assertEquals(saved.getUuid(), fetched.getUuid());
	}
}
