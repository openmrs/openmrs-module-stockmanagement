/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement;

import org.openmrs.module.stockmanagement.api.StockItemService;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.search.StockItemSearch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.*;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.module.stockmanagement.api.dto.*;
import org.openmrs.module.stockmanagement.api.jobs.StockItemImportJob;
import org.openmrs.module.stockmanagement.api.model.*;
import org.openmrs.module.stockmanagement.api.utils.DateUtil;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import org.openmrs.api.db.hibernate.search.session.SearchSessionFactory;

public class StockItemServiceTest extends BaseModuleContextSensitiveTest {
	
	private void deleteAllStockItems() {
		dao().getSession().createQuery("delete from stockmanagement.StockItem").executeUpdate();
	}
	
	@Test
	public void itemSearchShouldPreservePredicatesAndPagination() {
		StockItem first = eu().newStockItem(dao(), true);
		first.setVoided(false);
		first.setCategory(Context.getConceptService().getConcept(6));
		Context.getService(StockItemService.class).saveStockItem(first);
		StockItem second = eu().newStockItem(dao(), false);
		second.setVoided(true);
		Context.getService(StockItemService.class).saveStockItem(second);
		StockItemSearch filter = new StockItemSearch();
		filter.setStockItemIds(Arrays.asList(first.getId(), second.getId()));
		filter.setStartIndex(0);
		filter.setLimit(1);
		assertEquals(first.getUuid(),
		    Context.getService(StockItemService.class).findStockItems(filter).getData().get(0).getUuid());
		filter.setIncludeVoided(true);
		assertEquals(2, Context.getService(StockItemService.class).findStockItems(filter).getTotalRecordCount());
		filter.setStartIndex(1);
		assertEquals(second.getUuid(),
		    Context.getService(StockItemService.class).findStockItems(filter).getData().get(0).getUuid());
		filter.setStartIndex(0);
		filter.setCategoryId(6);
		filter.setDrugId(first.getDrug().getId());
		filter.setIsDrug(true);
		assertEquals(first.getUuid(),
		    Context.getService(StockItemService.class).findStockItems(filter).getData().get(0).getUuid());
		filter.setIsDrug(false);
		assertTrue(Context.getService(StockItemService.class).findStockItems(filter).getData().isEmpty());
	}
	
	@Test
	public void batchIdentityShouldMatchCaseInsensitiveNumberAndExactOrNullExpiry() {
		StockBatch batch = eu().newStockBatch(dao());
		batch.setVoided(false);
		batch.setBatchNo("BATCH-6317");
		batch.setExpiration(null);
		Context.getService(StockItemService.class).saveStockBatch(batch);
		assertEquals(batch.getUuid(),
		    Context.getService(StockItemService.class).findStockBatch(batch.getStockItem(), "batch-6317", null).getUuid());
		assertNull(
		    Context.getService(StockItemService.class).findStockBatch(batch.getStockItem(), "batch-6317", new Date()));
		Date expiration = DateUtil.today();
		batch.setExpiration(expiration);
		Context.getService(StockItemService.class).saveStockBatch(batch);
		assertNull(Context.getService(StockItemService.class).findStockBatch(batch.getStockItem(), "batch-6317", null));
		assertEquals(batch.getUuid(), Context.getService(StockItemService.class)
		        .findStockBatch(batch.getStockItem(), "batch-6317", expiration).getUuid());
		batch.setVoided(true);
		Context.getService(StockItemService.class).saveStockBatch(batch);
		assertNull(
		    Context.getService(StockItemService.class).findStockBatch(batch.getStockItem(), "batch-6317", expiration));
	}
	
	@Test
	public void referencesShouldSaveUpdateLookupAndVoid() {
		StockItem item = eu().newStockItem(dao(), true);
		Context.getService(StockItemService.class).saveStockItem(item);
		StockSource source = eu().newStockSource(dao());
		dao().saveStockSource(source);
		StockItemReference reference = new StockItemReference();
		reference.setStockItem(item);
		reference.setReferenceSource(source);
		reference.setStockReferenceCode("REF-6317");
		reference.setCreator(Context.getAuthenticatedUser());
		reference.setDateCreated(new Date());
		Context.getService(StockItemService.class).saveStockItemReference(reference);
		assertEquals(item, Context.getService(StockItemService.class).getStockItemByReference(source, "REF-6317"));
		assertEquals(reference, Context.getService(StockItemService.class).getStockItemReferenceByUuid(reference.getUuid()));
		reference.setStockReferenceCode("UPDATED");
		Context.getService(StockItemService.class).saveStockItemReference(reference);
		assertNull(Context.getService(StockItemService.class).getStockItemByReference(source, "REF-6317"));
		Context.getService(StockItemService.class).voidStockItemReference(reference.getUuid(), "test",
		    Context.getAuthenticatedUser().getId());
		Context.flushSession();
		Context.clearSession();
		assertTrue(Context.getService(StockItemService.class).getStockItemReferenceByUuid(reference.getUuid()).getVoided());
		assertTrue(Context.getService(StockItemService.class).getStockItemReferenceByStockItem(item.getUuid()).isEmpty());
		assertNull(Context.getService(StockItemService.class).getStockItemReferenceByStockItem("missing"));
	}
	
	@Test
	public void packagingShouldUpdateFactorAndVoid() {
		StockItemPackagingUOM unit = eu().newStockItemPackagingUOM(dao());
		unit.setVoided(false);
		unit.setFactor(BigDecimal.TEN);
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(unit);
		assertEquals(0, Context.getService(StockItemService.class).getStockItemPackagingUOMByUuid(unit.getUuid()).getFactor()
		        .compareTo(BigDecimal.TEN));
		Context.getService(StockItemService.class).voidStockItemPackagingUOM(unit.getUuid(), "test",
		    Context.getAuthenticatedUser().getId());
		Context.flushSession();
		Context.clearSession();
		assertTrue(Context.getService(StockItemService.class).getStockItemPackagingUOMByUuid(unit.getUuid()).getVoided());
		StockItemSearch filter = new StockItemSearch();
		filter.setUuid(unit.getUuid());
		assertTrue(Context.getService(StockItemService.class).findStockItemPackagingUOMs(filter).getData().isEmpty());
		filter.setIncludeVoided(true);
		assertEquals(1, Context.getService(StockItemService.class).findStockItemPackagingUOMs(filter).getData().size());
	}
	
	@Test
	public void importShouldReportValidationErrors(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
		Path file = directory.resolve("invalid.csv");
		java.nio.file.Files.write(file, Arrays.asList("invalid,,yes,,,,,,,,,,,,,", ",,no,,,,,,,,,,,,,"));
		ImportResult result = Context.getService(StockItemService.class).importStockItems(file, false);
		assertFalse(result.getSuccess());
		assertFalse(result.getErrors().isEmpty());
		assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Row 1")));
	}
	
	@Test
	public void deprecatedLookupsShouldStillFindItemsForTheBillingModule() {
		// billing 2.5.0 calls these on StockManagementService until O3-6324 moves them
		StockItem item = eu().newStockItem(dao(), true);
		item.setVoided(false);
		Context.getService(StockItemService.class).saveStockItem(item);
		Context.flushSession();
		assertNotNull(item.getDrug());
		StockManagementService legacy = Context.getService(StockManagementService.class);
		
		assertEquals(item.getId(), legacy.getStockItemByUuid(item.getUuid()).getId());
		assertTrue(legacy.getStockItemByDrug(item.getDrug().getDrugId()).stream()
		        .anyMatch(found -> found.getId().equals(item.getId())));
	}
	
	@Test
	public void commonNameSearchShouldUseIndexAndRespectDrugVoidedAndLimit() {
		StockItem item = eu().newStockItem(dao(), true);
		item.setVoided(false);
		item.setCommonName("Zebrapine medicinal");
		item.setAcronym("ZBP");
		Context.getService(StockItemService.class).saveStockItem(item);
		Context.flushSession();
		searchSessionFactory.getSearchSession().indexingPlan().addOrUpdate(item);
		searchSessionFactory.getSearchSession().indexingPlan().execute();
		searchSessionFactory.getSearchSession().workspace(StockItem.class).refresh();
		assertTrue(Context.getService(StockItemService.class).searchStockItemCommonName("Zebrapine", true, false, 10)
		        .stream().anyMatch(found -> found.getId().equals(item.getId())));
		assertTrue(Context.getService(StockItemService.class).searchStockItemCommonName("ZBP", true, false, 1).stream()
		        .anyMatch(found -> found.getId().equals(item.getId())));
		assertTrue(
		    Context.getService(StockItemService.class).searchStockItemCommonName("Zebrapine", false, false, 10).isEmpty());
		item.setVoided(true);
		Context.getService(StockItemService.class).saveStockItem(item);
		Context.flushSession();
		searchSessionFactory.getSearchSession().indexingPlan().addOrUpdate(item);
		searchSessionFactory.getSearchSession().indexingPlan().execute();
		searchSessionFactory.getSearchSession().workspace(StockItem.class).refresh();
		assertTrue(
		    Context.getService(StockItemService.class).searchStockItemCommonName("Zebrapine", true, false, 10).isEmpty());
		assertTrue(Context.getService(StockItemService.class).searchStockItemCommonName("Zebrapine", true, true, 10).stream()
		        .anyMatch(found -> found.getId().equals(item.getId())));
	}
	
	@Autowired
	SearchSessionFactory searchSessionFactory;
	
	@Autowired
	DbSessionFactory sessionFactory;
	
	private StockManagementDao daoInstance;
	
	private EntityUtil entityUtil;
	
	private StockManagementDao dao() {
		if (daoInstance == null) {
			daoInstance = new StockManagementDao();
			daoInstance.setSessionFactory(sessionFactory);
			org.openmrs.module.stockmanagement.api.db.hibernate.HibernateStockItemDAO items = new org.openmrs.module.stockmanagement.api.db.hibernate.HibernateStockItemDAO();
			items.setSessionFactory(sessionFactory);
			daoInstance.setStockItemDAO(items);
			daoInstance.setSearchSessionFactory(searchSessionFactory);
		}
		return daoInstance;
	}
	
	@BeforeEach
	public void setup() throws Exception {
		initializeInMemoryDatabase();
		executeDataSet(EntityUtil.STOCK_OPERATION_TYPE_DATA_SET);
	}
	
	private EntityUtil eu() {
		if (entityUtil == null) {
			Drug drug = Context.getConceptService().getDrug(3);
			Concept concept = drug.getConcept();
			Role role = Context.getUserService().getRole("Provider");
			Patient patient = Context.getPatientService().getPatient(2);
			User user = Context.getUserService().getUser(501);
			Location location = Context.getLocationService().getLocation(1);
			entityUtil = new EntityUtil(drug, user, location, role, concept, patient);
		}
		return entityUtil;
	}
	
	@Test
	public void findStockItemPackagingUOMs_shouldFilterOnAllCriteria() {
		StockItemPackagingUOM so = eu().newStockItemPackagingUOM(dao());
		so.setVoided(false);
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(so);
		StockItemSearch filter = new StockItemSearch();
		filter.setLimit(10);
		filter.setStartIndex(0);
		Result<StockItemPackagingUOM> result = Context.getService(StockItemService.class).findStockItemPackagingUOMs(filter);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		
		filter = new StockItemSearch();
		filter.setStockItemUuids(Arrays.asList(so.getStockItem().getUuid()));
		result = Context.getService(StockItemService.class).findStockItemPackagingUOMs(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockItemSearch();
		filter.setStockItemIds(Arrays.asList(so.getStockItem().getId()));
		result = Context.getService(StockItemService.class).findStockItemPackagingUOMs(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
	}
	
	@Test
	public void findStockBatchs_shouldFilterOnAllCriteria() {
		StockBatch so = eu().newStockBatch(dao());
		so.setVoided(false);
		Context.getService(StockItemService.class).saveStockBatch(so);
		
		StockItemSearch filter = new StockItemSearch();
		filter.setLimit(10);
		filter.setStartIndex(0);
		Result<StockBatch> result = Context.getService(StockItemService.class).findStockBatches(filter);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		
		filter = new StockItemSearch();
		filter.setStockItemUuid(so.getStockItem().getUuid());
		result = Context.getService(StockItemService.class).findStockBatches(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockItemSearch();
		filter.setStockItemId(so.getStockItem().getId());
		result = Context.getService(StockItemService.class).findStockBatches(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
	}
	
	@Test
	public void findStockItems_shouldFilterOnAllCriteria() {
		StockItem so = eu().newStockItem(dao(), true);
		so.setVoided(false);
		Context.getService(StockItemService.class).saveStockItem(so);
		
		StockItemSearch filter = new StockItemSearch();
		filter.setLimit(10);
		filter.setStartIndex(0);
		Result<StockItem> result = Context.getService(StockItemService.class).findStockItems(filter);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		
		filter = new StockItemSearch();
		filter.setDrugs(Arrays.asList(so.getDrug()));
		result = Context.getService(StockItemService.class).findStockItems(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockItemSearch();
		filter.setConcepts(Arrays.asList(so.getConcept()));
		result = Context.getService(StockItemService.class).findStockItems(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockItemSearch();
		filter.setSearchEitherDrugsOrConcepts(true);
		filter.setConcepts(Arrays.asList(so.getConcept()));
		filter.setDrugs(Arrays.asList(so.getDrug()));
		result = Context.getService(StockItemService.class).findStockItems(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockItemSearch();
		filter.setUuid(so.getUuid());
		result = Context.getService(StockItemService.class).findStockItems(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
	}
	
	@Test
	public void importStockItems() throws Exception {
		URL resource = getClass().getClassLoader().getResource(EntityUtil.STOCK_ITEMS_IMPORT_CSV);
		Path path = Paths.get(resource.toURI()).toFile().toPath();
		StockItemImportJob importJob = new StockItemImportJob(path, false);
		importJob.execute();
		ImportResult importResult = (ImportResult) importJob.getResult();
		if (!importResult.getSuccess()) {
			throw new Exception(String.join(", ", importResult.getErrors()));
		}
		assertTrue(importResult.getSuccess());
		
		StockItemSearch stockItemSearchFilter = new StockItemSearch();
		stockItemSearchFilter.setIncludeVoided(true);
		Result<StockItem> stockItemDTOs = Context.getService(StockItemService.class).findStockItems(stockItemSearchFilter);
		
		assertEquals(5, stockItemDTOs.getData().size());
		Optional<StockItem> stockItemOptional = stockItemDTOs.getData().stream()
		        .filter(p -> p.getDrug() != null && p.getDrug().getId().equals(2)).findFirst();
		assertTrue(stockItemOptional.isPresent(), "Drug Stock item is present");
		StockItem stockItem = stockItemOptional.get();
		assertTrue(stockItem.getHasExpiration());
		assertEquals(stockItem.getCommonName(), "TEST 2");
		assertNull(stockItem.getAcronym());
		assertEquals(stockItem.getPreferredVendor().getName(), "National Medical Stores");
		assertEquals(stockItem.getDispensingUnit().getId(), (Integer) 20);
		assertEquals(stockItem.getDispensingUnitPackagingUoM().getPackagingUom().getId(), (Integer) 20);
		assertEquals(stockItem.getReorderLevel().compareTo(BigDecimal.valueOf(50)), 0);
		assertEquals(stockItem.getReorderLevelUOM().getPackagingUom().getId(), (Integer) 20);
		assertEquals(stockItem.getPurchasePrice().compareTo(BigDecimal.valueOf(13)), 0);
		assertEquals(stockItem.getPurchasePriceUoM().getPackagingUom().getId(), (Integer) 20);
		
		stockItemOptional = stockItemDTOs.getData().stream().filter(p -> p.getConcept().getId().equals(5497)).findFirst();
		assertTrue(stockItemOptional.isPresent(), "Concenpt Stock item is present");
		stockItem = stockItemOptional.get();
		assertFalse(stockItem.getHasExpiration());
		assertEquals(stockItem.getCommonName(), "TEST 2");
		assertNull(stockItem.getAcronym());
		assertEquals(stockItem.getPreferredVendor().getName(), "Uganda Medical Stores");
		assertEquals(stockItem.getDispensingUnit().getId(), (Integer) 20);
		assertEquals(stockItem.getDispensingUnitPackagingUoM().getPackagingUom().getId(), (Integer) 20);
		assertEquals(stockItem.getReorderLevel().compareTo(BigDecimal.valueOf(50)), 0);
		assertEquals(stockItem.getReorderLevelUOM().getPackagingUom().getId(), (Integer) 20);
		assertEquals(stockItem.getPurchasePrice().compareTo(BigDecimal.valueOf(13)), 0);
		assertEquals(stockItem.getPurchasePriceUoM().getPackagingUom().getId(), (Integer) 20);
		
	}
	
	@Test
	public void getStockItemByDrug() {
		deleteAllStockItems();
		Context.flushSession();
		Context.flushSession();
		
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		
		List<StockItem> stockItemLeft = Context.getService(StockItemService.class)
		        .getStockItemByDrug(stockItem.getDrug().getId());
		assertNotNull(stockItemLeft);
		assertTrue(
		    stockItemLeft.stream().map(StockItem::getUuid).collect(Collectors.toList()).contains(stockItem.getUuid()));
	}
	
	@Test
	public void getStockItemByConcept() {
		StockItem stockItem = eu().newStockItem(dao(), true);
		stockItem.setDrug(null);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		
		List<StockItem> stockItemLeft = Context.getService(StockItemService.class)
		        .getStockItemByConcept(stockItem.getConcept().getId());
		assertNotNull(stockItemLeft);
		assertTrue(
		    stockItemLeft.stream().map(StockItem::getUuid).collect(Collectors.toList()).contains(stockItem.getUuid()));
	}
	
	@Test
	public void getStockItemPackagingUOMByConcept() {
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		
		StockItemPackagingUOM packagingUomLeft = Context.getService(StockItemService.class)
		        .getStockItemPackagingUOMByConcept(stockItem.getId(), packagingUom.getPackagingUom().getConceptId());
		assertNotNull(packagingUomLeft);
		assertEquals(packagingUom.getUuid(), packagingUomLeft.getUuid());
	}
	
	@Test
	public void getStockItemPackagingUOMByConceptStockItemIdAndConceptUuid() {
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		
		StockItemPackagingUOM packagingUomLeft = Context.getService(StockItemService.class)
		        .getStockItemPackagingUOMByConcept(stockItem.getId(), packagingUom.getPackagingUom().getUuid());
		assertNotNull(packagingUomLeft);
		assertEquals(packagingUom.getUuid(), packagingUomLeft.getUuid());
	}
	
	@Test
	public void getStockItemPackagingUOMByConceptByStockItemUuidAndConceptId() {
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		
		StockItemPackagingUOM packagingUomLeft = Context.getService(StockItemService.class)
		        .getStockItemPackagingUOMByConcept(stockItem.getUuid(), packagingUom.getPackagingUom().getConceptId());
		assertNotNull(packagingUomLeft);
		assertEquals(packagingUom.getUuid(), packagingUomLeft.getUuid());
	}
	
	@Test
	public void getStockItemPackagingUOMByConceptByStockItemUuidAndConceptUuid() {
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		
		StockItemPackagingUOM packagingUomLeft = Context.getService(StockItemService.class)
		        .getStockItemPackagingUOMByConcept(stockItem.getUuid(), packagingUom.getPackagingUom().getUuid());
		assertNotNull(packagingUomLeft);
		assertEquals(packagingUom.getUuid(), packagingUomLeft.getUuid());
	}
}
