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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.stockmanagement.api.*;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.module.stockmanagement.api.db.hibernate.HibernateStockItemDAO;
import org.openmrs.module.stockmanagement.api.model.*;
import org.openmrs.module.stockmanagement.api.search.StockItemSearch;
import org.openmrs.module.stockmanagement.api.utils.DateUtil;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional coverage for the extracted Spring service, beyond the original characterization suite.
 */
public class StockItemServiceRegressionTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	DbSessionFactory sessionFactory;
	
	private StockManagementDao dao() {
		HibernateStockItemDAO items = new HibernateStockItemDAO();
		items.setSessionFactory(sessionFactory);
		StockManagementDao dao = new StockManagementDao();
		dao.setSessionFactory(sessionFactory);
		dao.setStockItemDAO(items);
		return dao;
	}
	
	private EntityUtil eu() {
		Drug drug = Context.getConceptService().getDrug(3);
		return new EntityUtil(drug, Context.getUserService().getUser(501), Context.getLocationService().getLocation(1),
		        Context.getUserService().getRole("Provider"), drug.getConcept(), Context.getPatientService().getPatient(2));
	}
	
	@BeforeEach
	public void setup() throws Exception {
		initializeInMemoryDatabase();
		executeDataSet(EntityUtil.STOCK_MANAGEMENT_DATA_SET);
	}
	
	private void withOnlyPrivilege(String privilegeName, Runnable action) {
		Privilege privilege = Context.getUserService().getPrivilege(privilegeName);
		if (privilege == null) {
			privilege = Context.getUserService().savePrivilege(new Privilege(privilegeName, privilegeName));
		}
		Role role = new Role("Single stock privilege");
		role.addPrivilege(privilege);
		role = Context.getUserService().saveRole(role);
		User user = Context.getAuthenticatedUser();
		Set<Role> original = new HashSet<>(user.getRoles());
		try {
			user.setRoles(new HashSet<>(Collections.singletonList(role)));
			action.run();
		}
		finally {
			user.setRoles(original);
		}
	}
	
	@Test
	public void operationWriterShouldCreateAndResolveBatchesWithoutItemMutationPrivilege() {
		StockItemService items = Context.getService(StockItemService.class);
		StockItem item = eu().newStockItem(dao(), false);
		item.setVoided(false);
		items.saveStockItem(item);
		StockItemPackagingUOM unit = eu().newStockItemPackagingUOM(dao(), false, item);
		unit.setVoided(false);
		items.saveStockItemPackagingUOM(unit);
		StockBatch batch = eu().newStockBatch(dao(), item);
		batch.setVoided(false);
		batch.setExpiration(null);
		withOnlyPrivilege(Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE, () -> {
			items.saveStockBatch(batch);
			assertEquals(batch, items.getStockBatchByUuid(batch.getUuid()));
			assertEquals(batch, items.findStockBatch(item, batch.getBatchNo(), null));
			assertEquals(Collections.singletonList(item),
			    items.getStockItemsByUuids(Collections.singletonList(item.getUuid())));
			assertEquals(Collections.singletonList(unit),
			    items.getStockItemPackagingUOMsByUuids(Collections.singletonList(unit.getUuid())));
			assertThrows(org.openmrs.api.APIAuthenticationException.class, () -> items.saveStockItem(item));
		});
	}
	
	@Test
	public void operationReaderShouldFilterBatchesByLocationWithoutItemViewPrivilege() {
		StockItemService items = Context.getService(StockItemService.class);
		StockBatch batch = eu().newStockBatch(dao());
		batch.setVoided(false);
		items.saveStockBatch(batch);
		Party party = eu().newParty(dao());
		party.setLocation(Context.getLocationService().getLocation(1));
		party.setStockSource(null);
		dao().saveParty(party);
		StockItemSearch search = new StockItemSearch();
		search.setStockBatchUuid(batch.getUuid());
		search.setStartIndex(0);
		search.setLimit(10);
		search.setLocationUuid(party.getLocation().getUuid());
		search.setExcludeEmptyStock(true);
		// LocationService already required this core privilege before the extraction.
		Context.addProxyPrivilege(org.openmrs.util.PrivilegeConstants.GET_LOCATIONS);
		try {
			withOnlyPrivilege(Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS, () -> {
				assertTrue(items.findStockBatches(search).getData().isEmpty());
				// Compatibility: location filtering happens after pagination and keeps its count.
				assertEquals(1, items.findStockBatches(search).getTotalRecordCount());
				assertThrows(org.openmrs.api.APIAuthenticationException.class, () -> items.saveStockBatch(batch));
			});
		}
		finally {
			Context.removeProxyPrivilege(org.openmrs.util.PrivilegeConstants.GET_LOCATIONS);
		}
	}
	
	@Test
	public void operationReaderShouldGetItemCostsWithoutItemViewPrivilege() {
		StockManagementDao dao = dao();
		StockOperationItem line = eu().newStockOperationItem(dao);
		line.setVoided(false);
		dao.saveStockOperationItem(line);
		org.openmrs.module.stockmanagement.api.dto.StockOperationItemSearchFilter search = new org.openmrs.module.stockmanagement.api.dto.StockOperationItemSearchFilter();
		search.setUuid(line.getUuid());
		withOnlyPrivilege(Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS, () -> {
			assertEquals(1,
			    Context.getService(StockManagementService.class).getStockOperationItemCosts(search).getData().size());
			assertThrows(org.openmrs.api.APIAuthenticationException.class,
			    () -> Context.getService(StockItemService.class).getStockItemByUuid(line.getStockItem().getUuid()));
		});
	}
	
	@Test
	public void legacyEntitySearchShouldKeepItsDistinctPredicateSemantics() {
		StockItemService items = Context.getService(StockItemService.class);
		StockItem item = eu().newStockItem(dao(), false);
		item.setVoided(false);
		items.saveStockItem(item);
		StockItemSearch search = new StockItemSearch();
		search.setUuid(item.getUuid());
		search.setStockItemIds(Collections.singletonList(Integer.MAX_VALUE));
		assertTrue(items.findStockItems(search).getData().isEmpty());
		assertEquals(Collections.singletonList(item), items.findStockItemEntities(search).getData());
	}
	
	@Test
	public void serviceShouldDeclarePrivilegesAndRejectUnauthenticatedAccess() {
		for (java.lang.reflect.Method method : StockItemService.class.getDeclaredMethods()) {
			org.openmrs.annotation.Authorized authorized = method.getAnnotation(org.openmrs.annotation.Authorized.class);
			assertNotNull(authorized, method.getName());
			assertTrue(authorized.value().length > 0, method.getName());
			for (String privilege : authorized.value())
				assertTrue(Privileges.ALL.contains(privilege), method.getName());
		}
		Context.logout();
		try {
			assertThrows(org.openmrs.api.APIAuthenticationException.class,
			    () -> Context.getService(StockItemService.class).getStockItemByUuid("missing"));
			assertThrows(org.openmrs.api.APIAuthenticationException.class,
			    () -> Context.getService(StockItemService.class).saveStockItem(new StockItem()));
		}
		finally {
			Context.authenticate("admin", "test");
		}
	}
	
	@Test
	public void readerShouldReadButNotMutateItemsOrImport(@org.junit.jupiter.api.io.TempDir Path directory) {
		StockItem item = eu().newStockItem(dao(), true);
		Context.getService(StockItemService.class).saveStockItem(item);
		Privilege privilege = Context.getUserService().getPrivilege(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS);
		if (privilege == null)
			privilege = Context.getUserService()
			        .savePrivilege(new Privilege(Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, "View stock items"));
		Role reader = new Role("Stock item reader");
		reader.addPrivilege(privilege);
		reader = Context.getUserService().saveRole(reader);
		User user = Context.getAuthenticatedUser();
		Set<Role> original = new HashSet<>(user.getRoles());
		try {
			user.setRoles(new HashSet<>(Collections.singletonList(reader)));
			assertEquals(item, Context.getService(StockItemService.class).getStockItemByUuid(item.getUuid()));
			assertThrows(org.openmrs.api.APIAuthenticationException.class,
			    () -> Context.getService(StockItemService.class).saveStockItem(item));
			assertThrows(org.openmrs.api.APIAuthenticationException.class,
			    () -> Context.getService(StockItemService.class).importStockItems(directory.resolve("unused.csv"), false));
		}
		finally {
			user.setRoles(original);
		}
	}
	
	@Test
	public void bulkLookupsShouldPreserveVoidedAndGroupSemantics() {
		StockItemPackagingUOM unit = eu().newStockItemPackagingUOM(dao());
		unit.setVoided(false);
		unit.getStockItem().setVoided(false);
		Context.getService(StockItemService.class).saveStockItem(unit.getStockItem(), unit);
		StockItemService items = Context.getService(StockItemService.class);
		assertTrue(
		    items.getStockItems(Collections.singletonList(unit.getStockItem().getId())).contains(unit.getStockItem()));
		assertTrue(items.getStockItemsByUuids(Collections.singletonList(unit.getStockItem().getUuid()))
		        .contains(unit.getStockItem()));
		assertTrue(items.getStockItemPackagingUOMsByUuids(Collections.singletonList(unit.getUuid())).contains(unit));
		StockItemSearch.PackagingGroup group = new StockItemSearch.PackagingGroup(unit.getStockItem().getId(),
		        Collections.singletonList(unit.getPackagingUom().getId()));
		assertTrue(items.getStockItemPackagingUOMs(Collections.singletonList(group)).contains(unit));
		StockItemSearch.ItemGroupFilter itemGroup = new StockItemSearch.ItemGroupFilter();
		itemGroup.setDrugId(unit.getStockItem().getDrug().getId());
		itemGroup.setIsDrug(true);
		assertTrue(items.getExistingStockItemIds(Collections.singletonList(itemGroup)).contains(unit.getStockItem()));
		unit.getStockItem().setVoided(true);
		unit.setVoided(true);
		items.saveStockItem(unit.getStockItem(), unit);
		assertTrue(items.getStockItemsByUuids(Collections.singletonList(unit.getStockItem().getUuid())).isEmpty());
		assertTrue(items.getStockItemPackagingUOMsByUuids(Collections.singletonList(unit.getUuid())).isEmpty());
		assertTrue(
		    items.getStockItems(Collections.singletonList(unit.getStockItem().getId())).contains(unit.getStockItem()));
		assertEquals(unit,
		    items.getStockItemPackagingUOMByConcept(unit.getStockItem().getUuid(), unit.getPackagingUom().getId()));
		assertTrue(items.getExistingStockItemIds(Collections.singletonList(itemGroup)).contains(unit.getStockItem()));
		assertTrue(items.getStockItems(Collections.emptyList()).isEmpty());
		assertTrue(items.getStockItemPackagingUOMs(Collections.emptyList()).isEmpty());
		assertTrue(items.getExistingStockItemIds(Collections.emptyList()).isEmpty());
	}
	
	@Test
	public void batchSearchShouldFilterIdentityExpiryAndPageCounts() {
		StockBatch batch = eu().newStockBatch(dao());
		batch.setVoided(false);
		batch.setExpiration(DateUtil.today());
		StockItemService items = Context.getService(StockItemService.class);
		items.saveStockBatch(batch);
		assertEquals(batch, items.getStockBatchByUuid(batch.getUuid()));
		StockItemSearch filter = new StockItemSearch();
		filter.setStockBatchUuid(batch.getUuid());
		filter.setStockBatchIds(Collections.singletonList(batch.getId()));
		filter.setStartIndex(0);
		filter.setLimit(10);
		assertEquals(1, items.findStockBatches(filter).getTotalRecordCount());
		filter.setExcludeExpired(true);
		assertTrue(items.findStockBatches(filter).getData().isEmpty());
		batch.setExpiration(null);
		items.saveStockBatch(batch);
		assertEquals(batch, items.findStockBatches(filter).getData().get(0));
		batch.setVoided(true);
		items.saveStockBatch(batch);
		assertTrue(items.findStockBatches(filter).getData().isEmpty());
		filter.setIncludeVoided(true);
		assertEquals(batch, items.findStockBatches(filter).getData().get(0));
	}
	
}
