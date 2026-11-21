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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.hibernate.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.*;
import org.openmrs.api.LocationService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.stockmanagement.api.Privileges;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.stockmanagement.api.dto.*;
import org.openmrs.module.stockmanagement.api.dto.reporting.StockOperationLineItem;
import org.openmrs.module.stockmanagement.api.dto.reporting.StockOperationLineItemFilter;
import org.openmrs.module.stockmanagement.api.impl.StockManagementServiceImpl;
import org.openmrs.module.stockmanagement.api.jobs.StockItemImportJob;
import org.openmrs.module.stockmanagement.api.model.*;
import org.openmrs.module.stockmanagement.api.utils.DateUtil;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;
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
		stockManagementService.setDao(dao());
		StockItem first = eu().newStockItem(dao(), true);
		first.setVoided(false);
		first.setCategory(Context.getConceptService().getConcept(6));
		dao().saveStockItem(first);
		StockItem second = eu().newStockItem(dao(), false);
		second.setVoided(true);
		dao().saveStockItem(second);
		StockItemSearchFilter filter = new StockItemSearchFilter();
		filter.setStockItemIds(Arrays.asList(first.getId(), second.getId()));
		filter.setStartIndex(0);
		filter.setLimit(1);
		assertEquals(first.getUuid(), stockManagementService.findStockItems(filter).getData().get(0).getUuid());
		filter.setIncludeVoided(true);
		assertEquals(2, stockManagementService.findStockItems(filter).getTotalRecordCount());
		filter.setStartIndex(1);
		assertEquals(second.getUuid(), stockManagementService.findStockItems(filter).getData().get(0).getUuid());
		filter.setStartIndex(0);
		filter.setCategoryId(6);
		filter.setDrugId(first.getDrug().getId());
		filter.setIsDrug(true);
		assertEquals(first.getUuid(), stockManagementService.findStockItems(filter).getData().get(0).getUuid());
		filter.setIsDrug(false);
		assertTrue(stockManagementService.findStockItems(filter).getData().isEmpty());
	}
	
	@Test
	public void batchIdentityShouldMatchCaseInsensitiveNumberAndExactOrNullExpiry() {
		StockBatch batch = eu().newStockBatch(dao());
		batch.setVoided(false);
		batch.setBatchNo("BATCH-6317");
		batch.setExpiration(null);
		dao().saveStockBatch(batch);
		assertEquals(batch.getUuid(), dao().findStockBatch(batch.getStockItem(), "batch-6317", null).getUuid());
		assertNull(dao().findStockBatch(batch.getStockItem(), "batch-6317", new Date()));
		Date expiration = DateUtil.today();
		batch.setExpiration(expiration);
		dao().saveStockBatch(batch);
		assertNull(dao().findStockBatch(batch.getStockItem(), "batch-6317", null));
		assertEquals(batch.getUuid(), dao().findStockBatch(batch.getStockItem(), "batch-6317", expiration).getUuid());
		batch.setVoided(true);
		dao().saveStockBatch(batch);
		assertNull(dao().findStockBatch(batch.getStockItem(), "batch-6317", expiration));
	}
	
	@Test
	public void referencesShouldSaveUpdateLookupAndVoid() {
		stockManagementService.setDao(dao());
		StockItem item = eu().newStockItem(dao(), true);
		dao().saveStockItem(item);
		StockSource source = eu().newStockSource(dao());
		dao().saveStockSource(source);
		StockItemReference reference = new StockItemReference();
		reference.setStockItem(item);
		reference.setReferenceSource(source);
		reference.setStockReferenceCode("REF-6317");
		reference.setCreator(Context.getAuthenticatedUser());
		reference.setDateCreated(new Date());
		stockManagementService.saveStockItemReference(reference);
		assertEquals(item, stockManagementService.getStockItemByReference(source, "REF-6317"));
		assertEquals(reference, stockManagementService.getStockItemReferenceByUuid(reference.getUuid()));
		reference.setStockReferenceCode("UPDATED");
		stockManagementService.saveStockItemReference(reference);
		assertNull(stockManagementService.getStockItemByReference(source, "REF-6317"));
		stockManagementService.voidStockItemReference(reference.getUuid(), "test", Context.getAuthenticatedUser().getId());
		Context.flushSession();
		Context.clearSession();
		assertTrue(stockManagementService.getStockItemReferenceByUuid(reference.getUuid()).getVoided());
		assertTrue(stockManagementService.getStockItemReferenceByStockItem(item.getUuid()).isEmpty());
		assertNull(stockManagementService.getStockItemReferenceByStockItem("missing"));
	}
	
	@Test
	public void packagingShouldUpdateFactorAndVoid() {
		stockManagementService.setDao(dao());
		StockItemPackagingUOM unit = eu().newStockItemPackagingUOM(dao());
		unit.setVoided(false);
		unit.setFactor(BigDecimal.TEN);
		stockManagementService.saveStockItemPackagingUOM(unit);
		assertEquals(0,
		    stockManagementService.getStockItemPackagingUOMByUuid(unit.getUuid()).getFactor().compareTo(BigDecimal.TEN));
		stockManagementService.voidStockItemPackagingUOM(unit.getUuid(), "test", Context.getAuthenticatedUser().getId());
		Context.flushSession();
		Context.clearSession();
		assertTrue(stockManagementService.getStockItemPackagingUOMByUuid(unit.getUuid()).getVoided());
		StockItemPackagingUOMSearchFilter filter = new StockItemPackagingUOMSearchFilter();
		filter.setUuid(unit.getUuid());
		assertTrue(stockManagementService.findStockItemPackagingUOMs(filter).getData().isEmpty());
		filter.setIncludeVoided(true);
		assertEquals(1, stockManagementService.findStockItemPackagingUOMs(filter).getData().size());
	}
	
	@Test
	public void importShouldReportValidationErrors(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
		Path file = directory.resolve("invalid.csv");
		java.nio.file.Files.write(file, Arrays.asList("invalid,,yes,,,,,,,,,,,,,", ",,no,,,,,,,,,,,,,"));
		ImportResult result = Context.getService(org.openmrs.module.stockmanagement.api.StockManagementService.class)
		        .importStockItems(file, false);
		assertFalse(result.getSuccess());
		assertFalse(result.getErrors().isEmpty());
		assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Row 1")));
	}
	
	@Test
	public void commonNameSearchShouldUseIndexAndRespectDrugVoidedAndLimit() {
		StockItem item = eu().newStockItem(dao(), true);
		item.setVoided(false);
		item.setCommonName("Zebrapine medicinal");
		item.setAcronym("ZBP");
		dao().saveStockItem(item);
		Context.flushSession();
		searchSessionFactory.getSearchSession().indexingPlan().addOrUpdate(item);
		searchSessionFactory.getSearchSession().indexingPlan().execute();
		searchSessionFactory.getSearchSession().workspace(StockItem.class).refresh();
		assertTrue(dao().searchStockItemCommonName("Zebrapine", true, false, 10).contains(item.getId()));
		assertTrue(dao().searchStockItemCommonName("ZBP", true, false, 1).contains(item.getId()));
		assertTrue(dao().searchStockItemCommonName("Zebrapine", false, false, 10).isEmpty());
		item.setVoided(true);
		dao().saveStockItem(item);
		Context.flushSession();
		searchSessionFactory.getSearchSession().indexingPlan().addOrUpdate(item);
		searchSessionFactory.getSearchSession().indexingPlan().execute();
		searchSessionFactory.getSearchSession().workspace(StockItem.class).refresh();
		assertTrue(dao().searchStockItemCommonName("Zebrapine", true, false, 10).isEmpty());
		assertTrue(dao().searchStockItemCommonName("Zebrapine", true, true, 10).contains(item.getId()));
	}
	
	@Autowired
	SearchSessionFactory searchSessionFactory;
	
	@InjectMocks
	StockManagementServiceImpl stockManagementService;
	
	@Autowired
	DbSessionFactory sessionFactory;
	
	private StockManagementDao daoInstance;
	
	private EntityUtil entityUtil;
	
	private StockManagementDao dao() {
		if (daoInstance == null) {
			daoInstance = new StockManagementDao();
			daoInstance.setSessionFactory(sessionFactory);
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
	
	//	@Mock
	//	StockManagementDao dao;
	//
	//	@Mock
	//	UserService userService;
	
	@BeforeEach
	public void setupMocks() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void findStockItemPackagingUOMs_shouldFilterOnAllCriteria() {
		stockManagementService.setDao(dao());
		StockItemPackagingUOM so = eu().newStockItemPackagingUOM(dao());
		so.setVoided(false);
		daoInstance.saveStockItemPackagingUOM(so);
		StockItemPackagingUOMSearchFilter filter = new StockItemPackagingUOMSearchFilter();
		filter.setLimit(10);
		filter.setStartIndex(0);
		Result<StockItemPackagingUOMDTO> result = stockManagementService.findStockItemPackagingUOMs(filter);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		
		filter = new StockItemPackagingUOMSearchFilter();
		filter.setStockItemUuids(Arrays.asList(so.getStockItem().getUuid()));
		result = stockManagementService.findStockItemPackagingUOMs(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockItemPackagingUOMSearchFilter();
		filter.setStockItemIds(Arrays.asList(so.getStockItem().getId()));
		result = stockManagementService.findStockItemPackagingUOMs(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
	}
	
	@Test
	public void findStockBatchs_shouldFilterOnAllCriteria() {
		stockManagementService.setDao(dao());
		StockBatch so = eu().newStockBatch(dao());
		so.setVoided(false);
		daoInstance.saveStockBatch(so);
		
		StockBatchSearchFilter filter = new StockBatchSearchFilter();
		filter.setLimit(10);
		filter.setStartIndex(0);
		Result<StockBatchDTO> result = stockManagementService.findStockBatches(filter);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		
		filter = new StockBatchSearchFilter();
		filter.setStockItemUuid(so.getStockItem().getUuid());
		result = stockManagementService.findStockBatches(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockBatchSearchFilter();
		filter.setStockItemId(so.getStockItem().getId());
		result = stockManagementService.findStockBatches(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
	}
	
	@Test
	public void findStockItems_shouldFilterOnAllCriteria() {
		stockManagementService.setDao(dao());
		StockItem so = eu().newStockItem(dao(), true);
		so.setVoided(false);
		daoInstance.saveStockItem(so);
		
		StockItemSearchFilter filter = new StockItemSearchFilter();
		filter.setLimit(10);
		filter.setStartIndex(0);
		Result<StockItemDTO> result = stockManagementService.findStockItems(filter);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		
		filter = new StockItemSearchFilter();
		filter.setDrugs(Arrays.asList(so.getDrug()));
		result = stockManagementService.findStockItems(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockItemSearchFilter();
		filter.setConcepts(Arrays.asList(so.getConcept()));
		result = stockManagementService.findStockItems(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockItemSearchFilter();
		filter.setSearchEitherDrugsOrConcepts(true);
		filter.setConcepts(Arrays.asList(so.getConcept()));
		filter.setDrugs(Arrays.asList(so.getDrug()));
		result = stockManagementService.findStockItems(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockItemSearchFilter();
		filter.setUuid(so.getUuid());
		result = stockManagementService.findStockItems(filter);
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
		
		StockItemSearchFilter stockItemSearchFilter = new StockItemSearchFilter();
		stockItemSearchFilter.setIncludeVoided(true);
		Result<StockItemDTO> stockItemDTOs = dao().findStockItems(stockItemSearchFilter);
		
		assertEquals(5, stockItemDTOs.getData().size());
		Optional<StockItemDTO> stockItemOptional = stockItemDTOs.getData().stream().filter(p -> p.getDrugId().equals(2))
		        .findFirst();
		assertTrue(stockItemOptional.isPresent(), "Drug Stock item is present");
		StockItemDTO stockItem = stockItemOptional.get();
		assertTrue(stockItem.getHasExpiration());
		assertEquals(stockItem.getCommonName(), "TEST 2");
		assertNull(stockItem.getAcronym());
		assertEquals(stockItem.getPreferredVendorName(), "National Medical Stores");
		assertEquals(stockItem.getDispensingUnitId(), (Integer) 20);
		assertEquals(stockItem.getDispensingUnitPackagingConceptId(), (Integer) 20);
		assertEquals(stockItem.getReorderLevel().compareTo(BigDecimal.valueOf(50)), 0);
		assertEquals(stockItem.getReorderLevelConceptId(), (Integer) 20);
		assertEquals(stockItem.getPurchasePrice().compareTo(BigDecimal.valueOf(13)), 0);
		assertEquals(stockItem.getPurchasePriceConceptId(), (Integer) 20);
		
		stockItemOptional = stockItemDTOs.getData().stream().filter(p -> p.getConceptId().equals(5497)).findFirst();
		assertTrue(stockItemOptional.isPresent(), "Concenpt Stock item is present");
		stockItem = stockItemOptional.get();
		assertFalse(stockItem.getHasExpiration());
		assertEquals(stockItem.getCommonName(), "TEST 2");
		assertNull(stockItem.getAcronym());
		assertEquals(stockItem.getPreferredVendorName(), "Uganda Medical Stores");
		assertEquals(stockItem.getDispensingUnitId(), (Integer) 20);
		assertEquals(stockItem.getDispensingUnitPackagingConceptId(), (Integer) 20);
		assertEquals(stockItem.getReorderLevel().compareTo(BigDecimal.valueOf(50)), 0);
		assertEquals(stockItem.getReorderLevelConceptId(), (Integer) 20);
		assertEquals(stockItem.getPurchasePrice().compareTo(BigDecimal.valueOf(13)), 0);
		assertEquals(stockItem.getPurchasePriceConceptId(), (Integer) 20);
		
	}
	
	@Test
	public void getStockItemByDrug() {
		deleteAllStockItems();
		Context.flushSession();
		Context.flushSession();
		
		StockItem stockItem = eu().newStockItem(dao(), false);
		dao().saveStockItem(stockItem);
		
		stockManagementService.setDao(dao());
		List<StockItem> stockItemLeft = stockManagementService.getStockItemByDrug(stockItem.getDrug().getId());
		assertNotNull(stockItemLeft);
		assertTrue(
		    stockItemLeft.stream().map(StockItem::getUuid).collect(Collectors.toList()).contains(stockItem.getUuid()));
	}
	
	@Test
	public void getStockItemByConcept() {
		StockItem stockItem = eu().newStockItem(dao(), true);
		stockItem.setDrug(null);
		dao().saveStockItem(stockItem);
		
		stockManagementService.setDao(dao());
		List<StockItem> stockItemLeft = stockManagementService.getStockItemByConcept(stockItem.getConcept().getId());
		assertNotNull(stockItemLeft);
		assertTrue(
		    stockItemLeft.stream().map(StockItem::getUuid).collect(Collectors.toList()).contains(stockItem.getUuid()));
	}
	
	@Test
	public void getStockItemPackagingUOMByConcept() {
		stockManagementService.setDao(dao());
		StockItem stockItem = eu().newStockItem(dao(), false);
		dao().saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		dao().saveStockItemPackagingUOM(packagingUom);
		
		StockItemPackagingUOM packagingUomLeft = stockManagementService.getStockItemPackagingUOMByConcept(stockItem.getId(),
		    packagingUom.getPackagingUom().getConceptId());
		assertNotNull(packagingUomLeft);
		assertEquals(packagingUom.getUuid(), packagingUomLeft.getUuid());
	}
	
	@Test
	public void getStockItemPackagingUOMByConceptStockItemIdAndConceptUuid() {
		stockManagementService.setDao(dao());
		StockItem stockItem = eu().newStockItem(dao(), false);
		dao().saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		dao().saveStockItemPackagingUOM(packagingUom);
		
		StockItemPackagingUOM packagingUomLeft = stockManagementService.getStockItemPackagingUOMByConcept(stockItem.getId(),
		    packagingUom.getPackagingUom().getUuid());
		assertNotNull(packagingUomLeft);
		assertEquals(packagingUom.getUuid(), packagingUomLeft.getUuid());
	}
	
	@Test
	public void getStockItemPackagingUOMByConceptByStockItemUuidAndConceptId() {
		stockManagementService.setDao(dao());
		StockItem stockItem = eu().newStockItem(dao(), false);
		dao().saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		dao().saveStockItemPackagingUOM(packagingUom);
		
		StockItemPackagingUOM packagingUomLeft = stockManagementService
		        .getStockItemPackagingUOMByConcept(stockItem.getUuid(), packagingUom.getPackagingUom().getConceptId());
		assertNotNull(packagingUomLeft);
		assertEquals(packagingUom.getUuid(), packagingUomLeft.getUuid());
	}
	
	@Test
	public void getStockItemPackagingUOMByConceptByStockItemUuidAndConceptUuid() {
		stockManagementService.setDao(dao());
		StockItem stockItem = eu().newStockItem(dao(), false);
		dao().saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		dao().saveStockItemPackagingUOM(packagingUom);
		
		StockItemPackagingUOM packagingUomLeft = stockManagementService
		        .getStockItemPackagingUOMByConcept(stockItem.getUuid(), packagingUom.getPackagingUom().getUuid());
		assertNotNull(packagingUomLeft);
		assertEquals(packagingUom.getUuid(), packagingUomLeft.getUuid());
	}
}
