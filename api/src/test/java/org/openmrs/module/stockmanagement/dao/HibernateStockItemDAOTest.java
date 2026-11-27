/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.dao;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.api.db.hibernate.search.session.SearchSessionFactory;
import org.openmrs.module.stockmanagement.EntityUtil;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.module.stockmanagement.api.db.hibernate.HibernateStockItemDAO;
import org.openmrs.module.stockmanagement.api.dto.Result;
import org.openmrs.module.stockmanagement.api.model.*;
import org.openmrs.module.stockmanagement.api.search.StockItemSearch;
import org.openmrs.module.stockmanagement.api.utils.DateUtil;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for {@link HibernateStockItemDAO} directly against the in-memory H2 database.
 */
public class HibernateStockItemDAOTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	DbSessionFactory sessionFactory;
	
	@Autowired
	SearchSessionFactory searchSessionFactory;
	
	private StockManagementDao daoInstance;
	
	private EntityUtil entityUtil;
	
	private HibernateStockItemDAO itemDao() {
		HibernateStockItemDAO dao = new HibernateStockItemDAO();
		dao.setSessionFactory(sessionFactory);
		dao.setSearchSessionFactory(searchSessionFactory);
		return dao;
	}
	
	private StockManagementDao dao() {
		if (daoInstance == null) {
			daoInstance = new StockManagementDao();
			daoInstance.setSessionFactory(sessionFactory);
			daoInstance.setStockItemDAO(itemDao());
			daoInstance.setSearchSessionFactory(searchSessionFactory);
		}
		return daoInstance;
	}
	
	@BeforeEach
	public void setup() throws Exception {
		initializeInMemoryDatabase();
		executeDataSet(EntityUtil.STOCK_MANAGEMENT_DATA_SET);
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
	public void saveStockItem_shouldSaveAllProperties() {
		StockItem stockItem = eu().newStockItem(dao(), true);
		stockItem.setDateCreated(DateUtils.truncate(stockItem.getDateCreated(), Calendar.SECOND));
		stockItem.setDateChanged(DateUtils.truncate(stockItem.getDateChanged(), Calendar.SECOND));
		stockItem.setDateVoided(DateUtils.truncate(stockItem.getDateVoided(), Calendar.SECOND));
		
		itemDao().saveStockItem(stockItem);
		
		Context.flushSession();
		Context.clearSession();
		
		StockItem savedStockItem = itemDao().getStockItemByUuid(stockItem.getUuid());
		assertNotNull(savedStockItem);
		assertThat(savedStockItem, hasProperty("uuid", is(stockItem.getUuid())));
		assertThat(savedStockItem, hasProperty("commonName", is(stockItem.getCommonName())));
		assertThat(savedStockItem, hasProperty("acronym", is(stockItem.getAcronym())));
		assertThat(savedStockItem, hasProperty("hasExpiration", is(stockItem.getHasExpiration())));
		assertThat(savedStockItem, hasProperty("expiryNotice", is(stockItem.getExpiryNotice())));
		assertThat(savedStockItem, hasProperty("voided", is(stockItem.getVoided())));
		assertThat(savedStockItem, hasProperty("voidReason", is(stockItem.getVoidReason())));
	}
	
	@Test
	public void saveStockItemPackagingUOM_shouldSaveAllProperties() {
		StockItem stockItem = eu().newStockItem(dao(), false);
		itemDao().saveStockItem(stockItem);
		
		StockItemPackagingUOM uom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		uom.setFactor(BigDecimal.valueOf(10));
		uom.setVoided(false);
		
		itemDao().saveStockItemPackagingUOM(uom);
		
		Context.flushSession();
		Context.clearSession();
		
		StockItemPackagingUOM saved = itemDao().getStockItemPackagingUOMByUuid(uom.getUuid());
		assertNotNull(saved);
		assertThat(saved, hasProperty("uuid", is(uom.getUuid())));
		assertEquals(0, BigDecimal.valueOf(10).compareTo(saved.getFactor()));
		assertFalse(saved.getVoided());
		assertEquals(stockItem.getId(), saved.getStockItem().getId());
	}
	
	@Test
	public void saveStockBatch_shouldSaveAllProperties() {
		StockItem stockItem = eu().newStockItem(dao(), true);
		itemDao().saveStockItem(stockItem);
		
		StockBatch batch = eu().newStockBatch(dao());
		batch.setStockItem(stockItem);
		batch.setBatchNo("BATCH-DAO-001");
		batch.setExpiration(DateUtil.today());
		batch.setVoided(false);
		
		itemDao().saveStockBatch(batch);
		
		Context.flushSession();
		Context.clearSession();
		
		StockBatch saved = itemDao().getStockBatchByUuid(batch.getUuid());
		assertNotNull(saved);
		assertThat(saved, hasProperty("uuid", is(batch.getUuid())));
		assertEquals("BATCH-DAO-001", saved.getBatchNo());
		assertFalse(saved.getVoided());
		assertEquals(stockItem.getId(), saved.getStockItem().getId());
	}
	
	@Test
	public void saveStockItemReference_shouldSaveAllProperties() {
		StockItem stockItem = eu().newStockItem(dao(), true);
		itemDao().saveStockItem(stockItem);
		
		StockSource source = eu().newStockSource(dao());
		dao().saveStockSource(source);
		
		StockItemReference reference = new StockItemReference();
		reference.setStockItem(stockItem);
		reference.setReferenceSource(source);
		reference.setStockReferenceCode("REF-DAO-TEST");
		reference.setCreator(Context.getAuthenticatedUser());
		reference.setDateCreated(new Date());
		reference.setVoided(false);
		
		itemDao().saveStockItemReference(reference);
		
		Context.flushSession();
		Context.clearSession();
		
		StockItemReference saved = itemDao().getStockItemReferenceByUuid(reference.getUuid());
		assertNotNull(saved);
		assertEquals("REF-DAO-TEST", saved.getStockReferenceCode());
		assertEquals(source.getId(), saved.getReferenceSource().getId());
		assertEquals(stockItem.getId(), saved.getStockItem().getId());
		assertFalse(saved.getVoided());
		
		StockItemReference byRef = itemDao().getStockItemByReference(source, "REF-DAO-TEST");
		assertNotNull(byRef);
		assertEquals(stockItem.getId(), byRef.getStockItem().getId());
	}
	
	@Test
	public void findStockBatch_shouldMatchCaseInsensitiveBatchNoAndExpiry() {
		StockItem stockItem = eu().newStockItem(dao(), true);
		itemDao().saveStockItem(stockItem);
		
		StockBatch batch = eu().newStockBatch(dao());
		batch.setStockItem(stockItem);
		batch.setBatchNo("CASE-INSENSITIVE-123");
		Date today = DateUtil.today();
		batch.setExpiration(today);
		batch.setVoided(false);
		itemDao().saveStockBatch(batch);
		
		StockBatch found = itemDao().findStockBatch(stockItem, "case-insensitive-123", today);
		assertNotNull(found);
		assertEquals(batch.getUuid(), found.getUuid());
		
		assertNull(itemDao().findStockBatch(stockItem, "case-insensitive-123", DateUtils.addDays(today, 1)));
	}
	
	@Test
	public void voidPackagingUOMAndReference_shouldMarkVoided() {
		StockItem stockItem = eu().newStockItem(dao(), true);
		itemDao().saveStockItem(stockItem);
		
		StockItemPackagingUOM uom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		uom.setVoided(false);
		itemDao().saveStockItemPackagingUOM(uom);
		
		StockSource source = eu().newStockSource(dao());
		dao().saveStockSource(source);
		
		StockItemReference reference = new StockItemReference();
		reference.setStockItem(stockItem);
		reference.setReferenceSource(source);
		reference.setStockReferenceCode("REF-VOID");
		reference.setCreator(Context.getAuthenticatedUser());
		reference.setDateCreated(new Date());
		reference.setVoided(false);
		itemDao().saveStockItemReference(reference);
		
		Context.flushSession();
		Context.clearSession();
		
		itemDao().voidStockItemPackagingUOM(uom.getUuid(), "void reason", Context.getAuthenticatedUser().getId());
		itemDao().voidStockItemReference(reference.getUuid(), "void ref reason", Context.getAuthenticatedUser().getId());
		
		Context.flushSession();
		Context.clearSession();
		
		StockItemPackagingUOM voidedUom = itemDao().getStockItemPackagingUOMByUuid(uom.getUuid());
		assertTrue(voidedUom.getVoided());
		assertEquals("void reason", voidedUom.getVoidReason());
		
		StockItemReference voidedRef = itemDao().getStockItemReferenceByUuid(reference.getUuid());
		assertTrue(voidedRef.getVoided());
		assertEquals("void ref reason", voidedRef.getVoidReason());
	}
	
	@Test
	public void findStockItems_shouldSupportPaginationAndPredicates() {
		StockItem item1 = eu().newStockItem(dao(), true);
		item1.setVoided(false);
		itemDao().saveStockItem(item1);
		
		StockItem item2 = eu().newStockItem(dao(), false);
		item2.setVoided(true);
		itemDao().saveStockItem(item2);
		
		StockItemSearch search = new StockItemSearch();
		search.setStockItemIds(Arrays.asList(item1.getId(), item2.getId()));
		search.setIncludeVoided(false);
		search.setStartIndex(0);
		search.setLimit(10);
		
		Result<StockItem> activeResult = itemDao().findStockItems(search);
		assertEquals(1, activeResult.getData().size());
		assertEquals(item1.getUuid(), activeResult.getData().get(0).getUuid());
		
		search.setIncludeVoided(true);
		Result<StockItem> allResult = itemDao().findStockItems(search);
		assertEquals(2, allResult.getData().size());
	}
}
