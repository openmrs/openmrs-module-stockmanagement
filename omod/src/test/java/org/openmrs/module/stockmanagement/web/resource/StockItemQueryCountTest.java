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

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.StockItemService;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.Result;
import org.openmrs.module.stockmanagement.api.dto.StockItemDTO;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.module.stockmanagement.api.model.StockItemPackagingUOM;
import org.openmrs.module.stockmanagement.api.model.StockSource;
import org.openmrs.module.stockmanagement.api.search.StockItemSearch;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;

import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Listing stock items must not issue extra queries per item. */
public class StockItemQueryCountTest extends BaseModuleWebContextSensitiveTest {
	
	private final StockItemRestMapper mapper = new StockItemRestMapper();
	
	private int created = 0;
	
	@BeforeEach
	public void setup() throws Exception {
		initializeInMemoryDatabase();
	}
	
	private void addItems(int count) {
		StockItemService service = Context.getService(StockItemService.class);
		for (int i = 0; i < count; i++) {
			StockSource vendor = new StockSource();
			vendor.setName("Query count vendor");
			vendor.setAcronym("QC");
			vendor.setSourceType(Context.getConceptService().getConcept(3));
			Context.getService(StockManagementService.class).saveStockSource(vendor);
			StockItem item = new StockItem();
			item.setConcept(newConcept("Query count concept " + created));
			item.setCategory(newConcept("Query count category " + created));
			item.setIsDrug(false);
			item.setCommonName("Query count item " + created);
			item.setDispensingUnit(newConcept("Query count dispensing unit " + created));
			item.setPreferredVendor(vendor);
			service.saveStockItem(item);
			StockItemPackagingUOM unit = new StockItemPackagingUOM();
			unit.setStockItem(item);
			unit.setPackagingUom(newConcept("Query count package " + created++));
			unit.setFactor(BigDecimal.TEN);
			service.saveStockItemPackagingUOM(unit);
			item.setPurchasePriceUoM(unit);
			item.setReorderLevelUOM(unit);
			item.setDispensingUnitPackagingUoM(unit);
			item.setDefaultStockOperationsUoM(unit);
			service.saveStockItem(item);
		}
	}
	
	private Concept newConcept(String name) {
		Concept concept = new Concept();
		concept.setDatatype(Context.getConceptService().getConceptDatatypeByName("N/A"));
		concept.setConceptClass(Context.getConceptService().getConceptClassByName("Misc"));
		ConceptName conceptName = new ConceptName(name, Locale.ENGLISH);
		conceptName.setConceptNameType(ConceptNameType.FULLY_SPECIFIED);
		concept.addName(conceptName);
		ConceptName translation = new ConceptName(name + " francais", Locale.FRENCH);
		translation.setConceptNameType(ConceptNameType.FULLY_SPECIFIED);
		concept.addName(translation);
		return Context.getConceptService().saveConcept(concept);
	}
	
	private long statementsToList() {
		SessionFactory sessionFactory = Context.getRegisteredComponent("sessionFactory", SessionFactory.class);
		sessionFactory.getCurrentSession().flush();
		sessionFactory.getCurrentSession().clear();
		sessionFactory.getCache().evictAllRegions();
		Statistics statistics = sessionFactory.getStatistics();
		boolean statisticsEnabled = statistics.isStatisticsEnabled();
		statistics.setStatisticsEnabled(true);
		statistics.clear();
		try {
			StockItemSearch search = new StockItemSearch();
			search.setStartIndex(0);
			search.setLimit(100);
			Result<StockItemDTO> result = mapper.findStockItems(search);
			assertEquals(created, result.getData().size());
			assertEquals(created, result.getTotalRecordCount());
			return statistics.getPrepareStatementCount();
		}
		finally {
			statistics.setStatisticsEnabled(statisticsEnabled);
		}
	}
	
	@Test
	public void listingShouldUseTheSameNumberOfQueriesForFewOrManyItems() {
		addItems(2);
		long few = statementsToList();
		addItems(20);
		long many = statementsToList();
		assertEquals(few, many, "statements for 2 items vs 22 items");
		// Page, count, related rows and concept names, plus the creator user that core always loads with
		// BaseOpenmrsData (with its roles and person). The old DTO projection used 4.
		assertTrue(many <= 12, "statements for 22 items: " + many);
	}
	
	@Test
	public void listingShouldPreservePagingWithMultipleConceptNames() {
		addItems(6);
		SessionFactory sessionFactory = Context.getRegisteredComponent("sessionFactory", SessionFactory.class);
		sessionFactory.getCurrentSession().flush();
		sessionFactory.getCurrentSession().clear();
		StockItemSearch search = new StockItemSearch();
		search.setStartIndex(1);
		search.setLimit(2);
		Result<StockItemDTO> result = mapper.findStockItems(search);
		assertEquals(6, result.getTotalRecordCount());
		assertEquals(2, result.getData().size());
		assertEquals("Query count item 2", result.getData().get(0).getCommonName());
		assertEquals("Query count item 3", result.getData().get(1).getCommonName());
		search.setStartIndex(3);
		result = mapper.findStockItems(search);
		assertEquals(6, result.getTotalRecordCount());
		assertTrue(result.getData().isEmpty());
	}
	
	@Test
	public void listingShouldKeepItemsWithoutOptionalDetails() {
		StockItem item = new StockItem();
		item.setIsDrug(false);
		item.setCommonName("Item without optional details");
		Context.getService(StockItemService.class).saveStockItem(item);
		SessionFactory sessionFactory = Context.getRegisteredComponent("sessionFactory", SessionFactory.class);
		sessionFactory.getCurrentSession().flush();
		sessionFactory.getCurrentSession().clear();
		StockItemDTO dto = mapper.findStockItems(new StockItemSearch()).getData().get(0);
		assertEquals(item.getUuid(), dto.getUuid());
		assertNull(dto.getConceptName());
		assertNull(dto.getCategoryName());
		assertNull(dto.getPurchasePriceUoMUuid());
	}
}
