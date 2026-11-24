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
import org.openmrs.*;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.Privileges;
import org.openmrs.module.stockmanagement.api.StockItemService;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.*;
import org.openmrs.module.stockmanagement.api.model.*;
import org.openmrs.module.stockmanagement.api.search.StockItemSearch;
import org.openmrs.module.stockmanagement.api.utils.StockItemFields;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** REST compatibility coverage for fields previously projected by StockManagementDao. */
public class StockItemRestMapperTest extends BaseModuleWebContextSensitiveTest {
	
	private final StockItemRestMapper mapper = new StockItemRestMapper();
	
	@BeforeEach
	public void setup() throws Exception {
		initializeInMemoryDatabase();
	}
	
	private StockItem itemWithUnits() {
		StockItemService service = Context.getService(StockItemService.class);
		StockItem item = new StockItem();
		item.setDrug(Context.getConceptService().getDrug(3));
		item.setConcept(item.getDrug().getConcept());
		item.setIsDrug(true);
		item.setCommonName("REST contract item");
		item.setAcronym("RCI");
		item.setHasExpiration(true);
		item.setExpiryNotice(60);
		item.setCategory(Context.getConceptService().getConcept(6));
		item.setDispensingUnit(Context.getConceptService().getConcept(3));
		StockSource source = new StockSource();
		source.setName("REST supplier");
		source.setAcronym("RS");
		source.setSourceType(Context.getConceptService().getConcept(3));
		Context.getService(StockManagementService.class).saveStockSource(source);
		item.setPreferredVendor(source);
		service.saveStockItem(item);
		StockItemPackagingUOM unit = new StockItemPackagingUOM();
		unit.setStockItem(item);
		unit.setPackagingUom(Context.getConceptService().getConcept(20));
		unit.setFactor(BigDecimal.TEN);
		service.saveStockItemPackagingUOM(unit);
		item.setPurchasePriceUoM(unit);
		item.setPurchasePrice(new BigDecimal("12.50"));
		item.setReorderLevelUOM(unit);
		item.setReorderLevel(new BigDecimal("40"));
		item.setDispensingUnitPackagingUoM(unit);
		item.setDefaultStockOperationsUoM(unit);
		return service.saveStockItem(item);
	}
	
	@Test
	public void itemClerkShouldCreateAnItemWithAPreferredVendor() {
		assertItemClerkCanSavePreferredVendor(false);
	}
	
	@Test
	public void itemClerkShouldEditAnItemWithAPreferredVendor() {
		assertItemClerkCanSavePreferredVendor(true);
	}
	
	@Test
	public void itemViewerShouldNotResolvePreferredVendors() {
		String vendorUuid = itemWithUnits().getPreferredVendor().getUuid();
		Role viewer = new Role("Item-only viewer");
		viewer.addPrivilege(new Privilege(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS));
		User user = Context.getAuthenticatedUser();
		Set<Role> originalRoles = new HashSet<>(user.getRoles());
		try {
			user.setRoles(Collections.singleton(viewer));
			assertThrows(APIAuthenticationException.class,
			    () -> Context.getService(StockItemService.class).getPreferredVendorByUuid(vendorUuid));
		}
		finally {
			user.setRoles(originalRoles);
		}
	}
	
	private void assertItemClerkCanSavePreferredVendor(boolean edit) {
		StockItem existing = itemWithUnits();
		String vendorUuid = existing.getPreferredVendor().getUuid();
		StockItemDTO dto = new StockItemDTO();
		if (edit) {
			dto.setUuid(existing.getUuid());
		} else {
			dto.setConceptUuid(Context.getConceptService().getConcept(6).getUuid());
		}
		dto.setPreferredVendorUuid(vendorUuid);
		dto.setHasExpiration(false);
		dto.setCommonName("Saved by item clerk");
		Role clerk = new Role("Item-only clerk");
		for (String name : Arrays.asList(Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE,
		    Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, "Get Concepts")) {
			Privilege privilege = Context.getUserService().getPrivilege(name);
			if (privilege == null) {
				privilege = Context.getUserService().savePrivilege(new Privilege(name, name));
			}
			clerk.addPrivilege(privilege);
		}
		Context.getUserService().saveRole(clerk);
		User user = Context.getAuthenticatedUser();
		Set<Role> originalRoles = new HashSet<>(user.getRoles());
		try {
			user.setRoles(Collections.singleton(clerk));
			assertEquals(3, user.getPrivileges().size());
			assertFalse(Context.hasPrivilege(Privileges.APP_STOCKMANAGEMENT_STOCKSOURCES));
			assertThrows(APIAuthenticationException.class,
			    () -> Context.getService(StockManagementService.class).getStockSourceByUuid(vendorUuid));
			StockItemDTO saved = new StockItemResource().save(dto);
			assertNotNull(saved.getUuid());
			StockItem reloaded = Context.getService(StockItemService.class).getStockItemByUuid(saved.getUuid());
			assertEquals(vendorUuid, reloaded.getPreferredVendor().getUuid());
			assertEquals("Saved by item clerk", reloaded.getCommonName());
			if (edit) {
				assertEquals(existing.getUuid(), saved.getUuid());
			}
		}
		finally {
			user.setRoles(originalRoles);
		}
	}
	
	@Test
	public void itemProjectionShouldPreserveEveryStoredFieldAndUnitIdentity() {
		StockItem item = itemWithUnits();
		StockItemSearch search = new StockItemSearch();
		search.setUuid(item.getUuid());
		search.setStartIndex(0);
		search.setLimit(1);
		Result<StockItemDTO> result = mapper.findStockItems(search);
		StockItemDTO dto = result.getData().get(0);
		assertEquals(1, result.getTotalRecordCount());
		assertEquals(item.getUuid(), dto.getUuid());
		assertEquals(item.getId(), dto.getId());
		assertEquals(item.getDrug().getId(), dto.getDrugId());
		assertEquals(item.getDrug().getUuid(), dto.getDrugUuid());
		assertEquals(item.getDrug().getName(), dto.getDrugName());
		assertEquals(item.getConcept().getId(), dto.getConceptId());
		assertEquals(item.getConcept().getUuid(), dto.getConceptUuid());
		assertEquals(item.getPreferredVendor().getId(), dto.getPreferredVendorId());
		assertEquals(item.getPreferredVendor().getUuid(), dto.getPreferredVendorUuid());
		assertEquals("REST supplier", dto.getPreferredVendorName());
		assertEquals("REST contract item", dto.getCommonName());
		assertEquals("RCI", dto.getAcronym());
		assertTrue(dto.getHasExpiration());
		assertFalse(dto.getVoided());
		assertEquals(Integer.valueOf(60), dto.getExpiryNotice());
		assertEquals(item.getPurchasePrice(), dto.getPurchasePrice());
		assertEquals(item.getReorderLevel(), dto.getReorderLevel());
		StockItemPackagingUOM unit = item.getPurchasePriceUoM();
		assertEquals(unit.getId(), dto.getPurchasePriceUoMId());
		assertEquals(unit.getUuid(), dto.getPurchasePriceUoMUuid());
		assertEquals(unit.getFactor(), dto.getPurchasePriceUoMFactor());
		assertEquals(unit.getPackagingUom().getId(), dto.getPurchasePriceConceptId());
		assertEquals(unit.getId(), dto.getReorderLevelUoMId());
		assertEquals(unit.getUuid(), dto.getReorderLevelUoMUuid());
		assertEquals(unit.getFactor(), dto.getReorderLevelUoMFactor());
		assertEquals(unit.getPackagingUom().getId(), dto.getReorderLevelConceptId());
		assertEquals(unit.getId(), dto.getDispensingUnitPackagingUoMId());
		assertEquals(unit.getUuid(), dto.getDispensingUnitPackagingUoMUuid());
		assertEquals(unit.getFactor(), dto.getDispensingUnitPackagingUoMFactor());
		assertEquals(unit.getPackagingUom().getId(), dto.getDispensingUnitPackagingConceptId());
		assertEquals(unit.getId(), dto.getDefaultStockOperationsUoMId());
		assertEquals(unit.getUuid(), dto.getDefaultStockOperationsUoMUuid());
		assertEquals(unit.getFactor(), dto.getDefaultStockOperationsUoMFactor());
		assertEquals(unit.getPackagingUom().getId(), dto.getDefaultStockOperationsConceptId());
		assertEquals(item.getDispensingUnit().getId(), dto.getDispensingUnitId());
		assertEquals(item.getDispensingUnit().getUuid(), dto.getDispensingUnitUuid());
		assertEquals(item.getCategory().getId(), dto.getCategoryId());
		assertEquals(item.getCategory().getUuid(), dto.getCategoryUuid());
		assertEquals(item.getCreator().getId(), dto.getCreator());
		assertEquals(item.getDateCreated(), dto.getDateCreated());
		assertNotNull(dto.getCreatorGivenName());
		assertNotNull(dto.getCreatorFamilyName());
		assertNotNull(dto.getConceptName());
		assertNotNull(dto.getDispensingUnitName());
		assertNotNull(dto.getCategoryName());
		assertEquals(dto.getPurchasePriceUoMName(), dto.getReorderLevelUoMName());
		assertEquals(dto.getPurchasePriceUoMName(), dto.getDispensingUnitPackagingUoMName());
		assertEquals(dto.getPurchasePriceUoMName(), dto.getDefaultStockOperationsUoMName());
	}
	
	@Test
	public void packagingProjectionShouldKeepOptionalDispensingFieldsAndLocaleFallback() {
		StockItem item = itemWithUnits();
		StockItemPackagingUOM unit = item.getPurchasePriceUoM();
		Concept packaging = conceptWithEnglishName("English package");
		unit.setPackagingUom(packaging);
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(unit);
		item.setDispensingUnit(conceptWithEnglishName("English dispensing unit"));
		Context.getService(StockItemService.class).saveStockItem(item);
		String fallback = "English package";
		ConceptName french = new ConceptName("Emballage francais", Locale.FRENCH);
		french.setConceptNameType(ConceptNameType.FULLY_SPECIFIED);
		packaging.addName(french);
		Context.getConceptService().saveConcept(packaging);
		StockItemSearch search = new StockItemSearch();
		search.setUuid(unit.getUuid());
		Locale previous = Context.getLocale();
		try {
			Context.setLocale(Locale.FRENCH);
			StockItemPackagingUOMDTO dto = mapper.findStockItemPackagingUOMs(search).getData().get(0);
			assertEquals("Emballage francais", dto.getPackagingUomName());
			assertEquals(unit.getFactor(), dto.getFactor());
			assertEquals(item.getId().intValue(), dto.getStockItemId());
			assertEquals(item.getUuid(), dto.getStockItemUuid());
			assertTrue(dto.getIsDefaultStockOperationsUoM());
			assertTrue(dto.getIsDispensingUnit());
			assertNull(dto.getStockItemDispensingUnitId());
			assertNull(dto.getStockItemDispensingUnitName());
			search.setIncludeDispensingUnit(true);
			dto = mapper.findStockItemPackagingUOMs(search).getData().get(0);
			assertEquals(item.getDispensingUnit().getId(), dto.getStockItemDispensingUnitId());
			assertEquals("English dispensing unit", dto.getStockItemDispensingUnitName());
			assertEquals(dto.getStockItemDispensingUnitName(), StockItemFields.packagingName(unit, true));
			Context.setLocale(Locale.GERMAN);
			assertEquals(fallback, mapper.findStockItemPackagingUOMs(search).getData().get(0).getPackagingUomName());
		}
		finally {
			Context.setLocale(previous);
		}
	}
	
	private Concept conceptWithEnglishName(String name) {
		Concept concept = new Concept();
		concept.setDatatype(Context.getConceptService().getConceptDatatypeByName("N/A"));
		concept.setConceptClass(Context.getConceptService().getConceptClassByName("Misc"));
		ConceptName conceptName = new ConceptName(name, Locale.ENGLISH);
		conceptName.setConceptNameType(ConceptNameType.FULLY_SPECIFIED);
		concept.addName(conceptName);
		return Context.getConceptService().saveConcept(concept);
	}
	
	@Test
	public void batchProjectionShouldPreserveExpiryFieldsVoidingAndPagingMetadata() {
		StockItem item = itemWithUnits();
		StockBatch batch = new StockBatch();
		batch.setStockItem(item);
		batch.setBatchNo("REST-BATCH");
		batch.setExpiration(new Date(1893456000000L));
		batch.setExpiryNotificationDate(new Date(1793456000000L));
		batch.setVoided(true);
		Context.getService(StockItemService.class).saveStockBatch(batch);
		StockItemSearch search = new StockItemSearch();
		search.setStockBatchUuid(batch.getUuid());
		search.setIncludeVoided(true);
		search.setStartIndex(0);
		search.setLimit(1);
		Result<StockBatchDTO> result = mapper.findStockBatches(search);
		StockBatchDTO dto = result.getData().get(0);
		assertEquals(1, result.getTotalRecordCount());
		assertEquals(Integer.valueOf(0), result.getPageIndex());
		assertEquals(Integer.valueOf(1), result.getPageSize());
		assertEquals(batch.getUuid(), dto.getUuid());
		assertEquals(batch.getId(), dto.getId());
		assertEquals("REST-BATCH", dto.getBatchNo());
		assertEquals(batch.getExpiration(), dto.getExpiration());
		assertEquals(batch.getExpiryNotificationDate(), dto.getExpiryNotificationDate());
		assertEquals(item.getUuid(), dto.getStockItemUuid());
		assertTrue(dto.isVoided());
	}
}
