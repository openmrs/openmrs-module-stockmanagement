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
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.*;
import org.openmrs.api.APIException;
import org.openmrs.api.LocationService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.stockmanagement.api.Privileges;
import org.openmrs.module.stockmanagement.api.StockItemService;
import org.openmrs.module.stockmanagement.api.dao.StockManagementDao;
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.stockmanagement.api.db.StockItemDAO;
import org.openmrs.module.stockmanagement.api.dto.*;
import org.openmrs.module.stockmanagement.api.dto.reporting.StockOperationLineItem;
import org.openmrs.module.stockmanagement.api.dto.reporting.StockOperationLineItemFilter;
import org.openmrs.module.stockmanagement.api.impl.StockManagementServiceImpl;
import org.openmrs.module.stockmanagement.api.jobs.StockItemImportJob;
import org.openmrs.module.stockmanagement.api.model.*;
import org.openmrs.module.stockmanagement.api.utils.DateUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * This is a unit test, which verifies logic in StockManagementService. It doesn't extend
 * BaseModuleContextSensitiveTest, thus it is run without the in-memory DB and Spring context.
 */
public class StockManagementServiceTest extends BaseModuleContextSensitiveTest {
	
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
			org.openmrs.module.stockmanagement.api.db.hibernate.HibernateStockItemDAO items = new org.openmrs.module.stockmanagement.api.db.hibernate.HibernateStockItemDAO();
			items.setSessionFactory(sessionFactory);
			daoInstance.setStockItemDAO(items);
			stockManagementService.setStockItemDAO(items);
		}
		return daoInstance;
	}
	
	@Before
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
	
	@Before
	public void setupMocks() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void findUserRoleScopes_shouldFilterOnAllCriteria() {
		stockManagementService.setDao(dao());
		UserRoleScope userRoleScope = eu().newUserRoleScope(dao());
		userRoleScope.setVoided(false);
		dao().saveUserRoleScope(userRoleScope);
		
		UserRoleScopeLocation userRoleScopeLocation = eu().newUserRoleScopeLocation(dao());
		UserRoleScope userRoleScope2 = userRoleScopeLocation.getUserRoleScope();
		userRoleScope2.setVoided(false);
		dao().saveUserRoleScope(userRoleScope2);
		userRoleScopeLocation.setVoided(false);
		dao().saveUserRoleScopeLocation(userRoleScopeLocation);
		
		UserRoleScopeOperationType userRoleScopeOperationType = eu().newUserRoleScopeOperationType(dao());
		UserRoleScope userRoleScope3 = userRoleScopeOperationType.getUserRoleScope();
		userRoleScope3.setVoided(false);
		dao().saveUserRoleScope(userRoleScope3);
		userRoleScopeOperationType.setVoided(false);
		dao().saveUserRoleScopeOperationType(userRoleScopeOperationType);
		userRoleScopeOperationType = dao().getUserRoleScopeOperationTypeByUuid(userRoleScopeOperationType.getUuid());
		
		//Let's clean up the cache to be sure getUserRoleScopeByUuid fetches from DB and not from cache
		Context.flushSession();
		Context.flushSession();
		
		UserRoleScopeSearchFilter filter = new UserRoleScopeSearchFilter();
		filter.setLimit(10);
		filter.setStartIndex(0);
		Result<UserRoleScopeDTO> result = stockManagementService.findUserRoleScopes(filter);
		assertThat(result.getData().size(), greaterThanOrEqualTo(3));
		assertTrue(
		    result.getData().stream().filter(p -> p.getUuid().equals(userRoleScope.getUuid())).findFirst().isPresent());
		assertTrue(
		    result.getData().stream().filter(p -> p.getUuid().equals(userRoleScopeLocation.getUserRoleScope().getUuid()))
		            .findFirst().isPresent());
		final UserRoleScopeOperationType finalUserRoleScopeOperationType = userRoleScopeOperationType;
		assertTrue(result.getData().stream()
		        .filter(p -> p.getUuid().equals(finalUserRoleScopeOperationType.getUserRoleScope().getUuid())).findFirst()
		        .isPresent());
		
		assertEquals(result.getData().stream().filter(p -> p.getUuid().equals(userRoleScope.getUuid())).findFirst().get()
		        .getUserFamilyName(),
		    userRoleScope.getUser().getFamilyName());
		assertEquals(
		    result.getData().stream().filter(p -> p.getUuid().equals(userRoleScopeLocation.getUserRoleScope().getUuid()))
		            .findFirst().get().getLocations().get(0).getLocationName(),
		    userRoleScopeLocation.getLocation().getName());
		assertEquals(
		    result.getData().stream()
		            .filter(p -> p.getUuid().equals(finalUserRoleScopeOperationType.getUserRoleScope().getUuid()))
		            .findFirst().get().getOperationTypes().get(0).getOperationTypeName(),
		    userRoleScopeOperationType.getStockOperationType().getName());
		
		filter = new UserRoleScopeSearchFilter();
		filter.setLocation(userRoleScopeLocation.getLocation());
		result = stockManagementService.findUserRoleScopes(filter);
		assertFalse(
		    result.getData().stream().filter(p -> p.getUuid().equals(userRoleScope.getUuid())).findFirst().isPresent());
		assertTrue(
		    result.getData().stream().filter(p -> p.getUuid().equals(userRoleScopeLocation.getUserRoleScope().getUuid()))
		            .findFirst().isPresent());
		assertFalse(result.getData().stream()
		        .filter(p -> p.getUuid().equals(finalUserRoleScopeOperationType.getUserRoleScope().getUuid())).findFirst()
		        .isPresent());
		
		filter = new UserRoleScopeSearchFilter();
		filter.setOperationType(userRoleScopeOperationType.getStockOperationType());
		result = stockManagementService.findUserRoleScopes(filter);
		assertFalse(
		    result.getData().stream().filter(p -> p.getUuid().equals(userRoleScope.getUuid())).findFirst().isPresent());
		assertFalse(
		    result.getData().stream().filter(p -> p.getUuid().equals(userRoleScopeLocation.getUserRoleScope().getUuid()))
		            .findFirst().isPresent());
		assertTrue(result.getData().stream()
		        .filter(p -> p.getUuid().equals(finalUserRoleScopeOperationType.getUserRoleScope().getUuid())).findFirst()
		        .isPresent());
		
		filter = new UserRoleScopeSearchFilter();
		filter.setName(userRoleScope.getUser().getFamilyName());
		result = stockManagementService.findUserRoleScopes(filter);
		assertTrue(
		    result.getData().stream().filter(p -> p.getUuid().equals(userRoleScope.getUuid())).findFirst().isPresent());
		assertTrue(
		    result.getData().stream().filter(p -> p.getUuid().equals(userRoleScopeLocation.getUserRoleScope().getUuid()))
		            .findFirst().isPresent());
		assertTrue(result.getData().stream()
		        .filter(p -> p.getUuid().equals(finalUserRoleScopeOperationType.getUserRoleScope().getUuid())).findFirst()
		        .isPresent());
		
		filter = new UserRoleScopeSearchFilter();
		filter.setName(UUID.randomUUID().toString());
		result = stockManagementService.findUserRoleScopes(filter);
		assertFalse(
		    result.getData().stream().filter(p -> p.getUuid().equals(userRoleScope.getUuid())).findFirst().isPresent());
		assertFalse(
		    result.getData().stream().filter(p -> p.getUuid().equals(userRoleScopeLocation.getUserRoleScope().getUuid()))
		            .findFirst().isPresent());
		assertFalse(result.getData().stream()
		        .filter(p -> p.getUuid().equals(finalUserRoleScopeOperationType.getUserRoleScope().getUuid())).findFirst()
		        .isPresent());
	}
	
	@Test
	public void findStockOperations_shouldFilterOnAllCriteria() {
		stockManagementService.setDao(dao());
		StockOperation so = eu().newStockOperation(dao());
		so.setVoided(false);
		so.setOperationNumber(so.getOperationNumber().toUpperCase());
		daoInstance.saveStockOperation(so);
		StockOperationSearchFilter filter = new StockOperationSearchFilter();
		filter.setLimit(10);
		filter.setStartIndex(0);
		Result<StockOperationDTO> result = dao().findStockOperations(filter, null);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		
		filter = new StockOperationSearchFilter();
		filter.setLocationId(so.getSource().getLocation().getLocationId());
		result = dao().findStockOperations(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setLocationId(so.getDestination().getLocation().getLocationId());
		result = dao().findStockOperations(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setLocationId(so.getAtLocation().getLocationId());
		result = dao().findStockOperations(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setOperationDateMax(so.getOperationDate());
		result = dao().findStockOperations(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setOperationDateMax(DateUtils.addDays(so.getOperationDate(), 1));
		result = dao().findStockOperations(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setOperationDateMax(DateUtils.addDays(so.getOperationDate(), -1));
		result = dao().findStockOperations(filter, null);
		assertFalse(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setOperationDateMin(so.getOperationDate());
		result = dao().findStockOperations(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setOperationDateMin(DateUtils.addDays(so.getOperationDate(), 1));
		result = dao().findStockOperations(filter, null);
		assertFalse(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setOperationDateMin(DateUtils.addDays(so.getOperationDate(), -1));
		result = dao().findStockOperations(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setOperationNumber(so.getOperationNumber());
		result = dao().findStockOperations(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setOperationTypeId(Arrays.asList(so.getStockOperationType().getId()));
		result = dao().findStockOperations(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setOperationTypeId(Arrays.asList(so.getStockOperationType().getId() + 3));
		result = stockManagementService.findStockOperations(filter);
		assertFalse(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setStatus(Arrays.asList(so.getStatus()));
		result = dao().findStockOperations(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setStatus(Arrays.asList(
		    Arrays.asList(StockOperationStatus.values()).stream().filter(p -> p != so.getStatus()).findFirst().get()));
		result = dao().findStockOperations(filter, null);
		assertFalse(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setStockItemId(Integer.MAX_VALUE - 10);
		result = dao().findStockOperations(filter, null);
		assertFalse(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setStockOperationUuid(so.getUuid());
		result = dao().findStockOperations(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setStockOperationUuid(so.getUuid() + "00");
		result = dao().findStockOperations(filter, null);
		assertFalse(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setPartyId(so.getSource().getId());
		result = dao().findStockOperations(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setPartyId(so.getDestination().getId());
		result = dao().findStockOperations(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setSourceTypeIds(Arrays.asList(Integer.MAX_VALUE));
		result = dao().findStockOperations(filter, null);
		assertFalse(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		String searchText = null;
		if (so.getDestination().getLocation() != null) {
			searchText = so.getDestination().getLocation().getName().substring(0,
			    Math.min(3, so.getDestination().getLocation().getName().length()));
		} else if (so.getDestination().getStockSource() != null) {
			searchText = so.getDestination().getStockSource().getName().substring(0,
			    Math.min(3, so.getDestination().getStockSource().getName().length()));
		}
		filter.setSearchText(searchText);
		result = dao().findStockOperations(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		searchText = null;
		if (so.getSource().getLocation() != null) {
			searchText = so.getDestination().getLocation().getName().substring(0,
			    Math.min(3, so.getSource().getLocation().getName().length()));
		} else if (so.getSource().getStockSource() != null) {
			searchText = so.getDestination().getStockSource().getName().substring(0,
			    Math.min(3, so.getSource().getStockSource().getName().length()));
		}
		filter.setSearchText(searchText);
		result = dao().findStockOperations(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		searchText = null;
		if (so.getSource().getLocation() != null) {
			searchText = so.getDestination().getLocation().getName().substring(0,
			    Math.min(3, so.getSource().getLocation().getName().length()));
		} else if (so.getSource().getStockSource() != null) {
			searchText = so.getDestination().getStockSource().getName().substring(0,
			    Math.min(3, so.getSource().getStockSource().getName().length()));
		}
		filter.setSearchText(searchText + "b_540");
		result = dao().findStockOperations(filter, null);
		assertFalse(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationSearchFilter();
		filter.setLocationId(so.getSource().getLocation().getLocationId());
		HashSet<RecordPrivilegeFilter> recordPrivilegeFilters = new HashSet<>();
		RecordPrivilegeFilter recordPrivilegeFilter = new RecordPrivilegeFilter();
		recordPrivilegeFilter.setOperationTypeId(Integer.MAX_VALUE);
		recordPrivilegeFilter.setLocationId(so.getDestination().getLocation().getId());
		recordPrivilegeFilters.add(recordPrivilegeFilter);
		result = dao().findStockOperations(filter, recordPrivilegeFilters);
		assertFalse(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
	}
	
	@Test
	public void findStockSources_shouldFilterOnAllCriteria() {
		stockManagementService.setDao(dao());
		StockSource so = eu().newStockSource(dao());
		so.setVoided(false);
		daoInstance.saveStockSource(so);
		StockSourceSearchFilter filter = new StockSourceSearchFilter();
		filter.setLimit(10);
		filter.setStartIndex(0);
		Result<StockSource> result = stockManagementService.findStockSources(filter);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		
		filter = new StockSourceSearchFilter();
		filter.setTextSearch(so.getName().substring(0, Math.min(3, so.getName().length())));
		result = stockManagementService.findStockSources(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockSourceSearchFilter();
		filter.setTextSearch(so.getName());
		result = stockManagementService.findStockSources(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockSourceSearchFilter();
		filter.setTextSearch(so.getAcronym().substring(0, Math.min(3, so.getAcronym().length())));
		result = stockManagementService.findStockSources(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockSourceSearchFilter();
		filter.setTextSearch(so.getAcronym());
		result = stockManagementService.findStockSources(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockSourceSearchFilter();
		filter.setSourceType(so.getSourceType());
		result = stockManagementService.findStockSources(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockSourceSearchFilter();
		filter.setIncludeVoided(so.getVoided());
		result = stockManagementService.findStockSources(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
	}
	
	@Test
	public void getAllDTOS_shouldHaveAllLocations() {
		stockManagementService.setDao(dao());
		List<PartyDTO> partyDTOs = stockManagementService.getAllParties();
	}
	
	@Test
	public void getFlattenedUserRoleScopesByUser_shouldHaveAllLocations() {
		stockManagementService.setDao(dao());
		dao().getFlattenedUserRoleScopesByUser(eu().getUser(), new HashSet<Role>(Arrays.asList(eu().getRole())), null, null);
	}
	
	@Test
	public void findStockOperationItems_shouldFilterOnAllCriteria() {
		stockManagementService.setDao(dao());
		StockOperationItem so = eu().newStockOperationItem(dao());
		so.setVoided(false);
		daoInstance.saveStockOperationItem(so);
		StockOperationItemSearchFilter filter = new StockOperationItemSearchFilter();
		filter.setIncludePackagingUnitName(true);
		filter.setIncludeStockUnitName(true);
		filter.setLimit(10);
		filter.setStartIndex(0);
		Result<StockOperationItemDTO> result = stockManagementService.findStockOperationItems(filter);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		
		filter = new StockOperationItemSearchFilter();
		filter.setIncludePackagingUnitName(true);
		filter.setIncludeStockUnitName(true);
		filter.setStockItemUuid(so.getStockItem().getUuid());
		result = stockManagementService.findStockOperationItems(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationItemSearchFilter();
		filter.setIncludePackagingUnitName(true);
		filter.setIncludeStockUnitName(true);
		filter.setStockOperationUuids(Arrays.asList(so.getStockOperation().getUuid()));
		result = stockManagementService.findStockOperationItems(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationItemSearchFilter();
		filter.setIncludePackagingUnitName(true);
		filter.setIncludeStockUnitName(true);
		filter.setStockOperationIds(Arrays.asList(so.getStockOperation().getId()));
		result = stockManagementService.findStockOperationItems(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
	}
	
	@Test
	public void findStockOperationItemCosts_shouldFilterOnAllCriteria() {
		stockManagementService.setDao(dao());
		StockOperationItem so = eu().newStockOperationItem(dao());
		so.setVoided(false);
		daoInstance.saveStockOperationItem(so);
		StockOperationItemSearchFilter filter = new StockOperationItemSearchFilter();
		filter.setIncludePackagingUnitName(true);
		filter.setIncludeStockUnitName(true);
		filter.setLimit(10);
		filter.setStartIndex(0);
		Result<StockOperationItemCost> result = stockManagementService.getStockOperationItemCosts(filter);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		
		filter = new StockOperationItemSearchFilter();
		filter.setIncludePackagingUnitName(true);
		filter.setIncludeStockUnitName(true);
		filter.setStockItemUuid(so.getStockItem().getUuid());
		result = stockManagementService.getStockOperationItemCosts(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationItemSearchFilter();
		filter.setIncludePackagingUnitName(true);
		filter.setIncludeStockUnitName(true);
		filter.setStockOperationUuids(Arrays.asList(so.getStockOperation().getUuid()));
		result = stockManagementService.getStockOperationItemCosts(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
		
		filter = new StockOperationItemSearchFilter();
		filter.setIncludePackagingUnitName(true);
		filter.setIncludeStockUnitName(true);
		filter.setStockOperationIds(Arrays.asList(so.getStockOperation().getId()));
		result = stockManagementService.getStockOperationItemCosts(filter);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(so.getUuid())));
	}
	
	@Test
	public void findStockOperationLinks_shouldFilterOnAllCriteria() {
		stockManagementService.setDao(dao());
		Result<StockOperationLinkDTO> result = stockManagementService.findStockOperationLinks(UUID.randomUUID().toString());
	}
	
	@Test
	public void getStockInventory_shouldFilterOnAllCriteria() {
		stockManagementService.setDao(dao());
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = entityUtil.newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		
		StockBatch stockBatch = eu().newStockBatch(dao(), stockItem);
		Context.getService(StockItemService.class).saveStockBatch(stockBatch);
		
		StockItemTransaction so = eu().newStockItemTransaction(dao(), stockItem, stockBatch, packagingUom);
		so.setQuantity(BigDecimal.valueOf(10));
		dao().saveStockItemTransaction(so);
		
		StockItemInventorySearchFilter filter = new StockItemInventorySearchFilter();
		List<StockItemInventorySearchFilter.ItemGroupFilter> itemsToSearch = new ArrayList<>();
		StockItemInventorySearchFilter.ItemGroupFilter itemGroupFilter = new StockItemInventorySearchFilter.ItemGroupFilter(
		        Arrays.asList(so.getParty().getId()), so.getStockItem().getId(), so.getStockBatch().getId());
		itemsToSearch.add(itemGroupFilter);
		filter.setItemGroupFilters(itemsToSearch);
		Result<StockItemInventory> result = stockManagementService.getStockInventory(filter, null);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		assertTrue(result.getData().get(0).getQuantity().setScale(2, BigDecimal.ROUND_HALF_EVEN)
		        .compareTo(so.getQuantity().multiply(BigDecimal.valueOf(2))) == 0);
		
		filter.setDoSetQuantityUoM(true);
		result = stockManagementService.getStockInventory(filter, null);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		//assertEquals(result.getData().get(0).getQuantity().setScale(2, BigDecimal.ROUND_HALF_EVEN),so.getQuantity().setScale(2, BigDecimal.ROUND_HALF_EVEN).multiply(packagingUom.getFactor()));
		//assertTrue(result.getData().get(0).getQuantity().setScale(2, BigDecimal.ROUND_HALF_EVEN).compareTo(so.getQuantity().setScale(2, BigDecimal.ROUND_HALF_EVEN)) == 0);
	}
	
	@Test
	public void findStockItemTransactions_shouldFilterOnAllCriteria() {
		stockManagementService.setDao(dao());
		StockItemTransaction so = eu().newStockItemTransaction(dao());
		so.setQuantity(BigDecimal.TEN);
		dao().saveStockItemTransaction(so);
		
		StockItemTransactionSearchFilter filter = new StockItemTransactionSearchFilter();
		filter.setStockItemId(so.getStockItem().getId());
		Result<StockItemTransactionDTO> result = dao().findStockItemTransactions(filter, null);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		assertTrue(
		    result.getData().get(0).getQuantity().setScale(2, BigDecimal.ROUND_HALF_EVEN).compareTo(so.getQuantity()) == 0);
		
		filter = new StockItemTransactionSearchFilter();
		filter.setUuid(so.getUuid());
		result = dao().findStockItemTransactions(filter, null);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		assertTrue(
		    result.getData().get(0).getQuantity().setScale(2, BigDecimal.ROUND_HALF_EVEN).compareTo(so.getQuantity()) == 0);
		assertEquals(result.getData().get(0).getUuid(), so.getUuid());
	}
	
	@Test
	public void findStockItemTransactions_shouldReturnPatientUuidWithoutGetPatientsPrivilege() {
		StockItemTransaction patientTransaction = eu().newStockItemTransaction(dao());
		patientTransaction.setPatient(eu().getPatient());
		dao().saveStockItemTransaction(patientTransaction);
		
		StockItemTransaction nonPatientTransaction = eu().newStockItemTransaction(dao(), patientTransaction.getStockItem(),
		    patientTransaction.getStockBatch(), patientTransaction.getStockItemPackagingUOM());
		nonPatientTransaction.setPatient(null);
		dao().saveStockItemTransaction(nonPatientTransaction);
		Context.flushSession();
		String expectedPatientUuid = eu().getPatient().getUuid();
		
		StockItemTransactionSearchFilter filter = new StockItemTransactionSearchFilter();
		filter.setStockItemId(patientTransaction.getStockItem().getId());
		
		Context.logout();
		Result<StockItemTransactionDTO> result;
		try {
			result = dao().findStockItemTransactions(filter, null);
		}
		finally {
			authenticate();
		}
		
		assertEquals(2, result.getData().size());
		assertEquals(expectedPatientUuid, result.getData().stream()
		        .filter(p -> p.getUuid().equals(patientTransaction.getUuid())).findFirst().get().getPatientUuid());
		assertNull(result.getData().stream().filter(p -> p.getUuid().equals(nonPatientTransaction.getUuid())).findFirst()
		        .get().getPatientUuid());
	}
	
	@Test
	public void dispenseStockItems() {
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = entityUtil.newStockItemPackagingUOM(dao(), false, stockItem);
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		
		StockBatch stockBatch = eu().newStockBatch(dao(), stockItem);
		Context.getService(StockItemService.class).saveStockBatch(stockBatch);
		
		for (int i = 3; i > 0; i--) {
			StockItemTransaction stockItemTransaction = eu().newStockItemTransaction(dao(), stockItem, stockBatch,
			    packagingUom);
			stockItemTransaction.setQuantity(BigDecimal.valueOf(200));
			stockItemTransaction.setPatient(null);
			dao().saveStockItemTransaction(stockItemTransaction);
		}
		
		List<DispenseRequest> dispenseRequests = new ArrayList<>();
		DispenseRequest dispenseRequest = new DispenseRequest();
		dispenseRequest.setPatientId(eu().getPatient().getId());
		dispenseRequest.setLocationUuid(eu().getLocation().getUuid());
		dispenseRequest.setQuantity(BigDecimal.valueOf(50));
		dispenseRequest.setStockItemUuid(stockItem.getUuid());
		dispenseRequest.setOrderId(eu().getOrder().getId());
		dispenseRequest.setEncounterId(eu().getEncounter().getId());
		dispenseRequest.setStockBatchUuid(stockBatch.getUuid());
		dispenseRequest.setStockItemPackagingUOMUuid(packagingUom.getUuid());
		dispenseRequests.add(dispenseRequest);
		
		dispenseRequest = new DispenseRequest();
		dispenseRequest.setPatientId(eu().getPatient().getId());
		dispenseRequest.setLocationUuid(eu().getLocation().getUuid());
		dispenseRequest.setQuantity(BigDecimal.valueOf(100));
		dispenseRequest.setStockItemUuid(stockItem.getUuid());
		dispenseRequests.add(dispenseRequest);
		dispenseRequest.setOrderId(eu().getOrder().getId());
		dispenseRequest.setEncounterId(eu().getEncounter().getId());
		dispenseRequest.setStockBatchUuid(stockBatch.getUuid());
		dispenseRequest.setStockItemPackagingUOMUuid(packagingUom.getUuid());
		stockManagementService.setDao(dao());
		
		stockManagementService.dispenseStockItems(dispenseRequests);
		
		StockItemTransactionSearchFilter searchFilter = new StockItemTransactionSearchFilter();
		searchFilter.setPartyId(eu().getParty(dao()).getId());
		searchFilter.setStockItemId(stockItem.getId());
		
		Result<StockItemTransactionDTO> result = dao().findStockItemTransactions(searchFilter, null);
		assertThat(result.getData().size(), greaterThanOrEqualTo(4));
		List<StockItemTransactionDTO> entity = result.getData().stream()
		        .filter(p -> dispenseRequests.get(0).getPatientId().equals(p.getPatientId())).collect(Collectors.toList());
		assertEquals(entity.size(), 2);
		assertTrue(entity.stream().anyMatch(p -> dispenseRequests.stream().anyMatch(x -> p.getQuantity()
		        .multiply(BigDecimal.valueOf(-1)).setScale(2, BigDecimal.ROUND_HALF_EVEN).compareTo(x.getQuantity()) == 0)));
	}
	
	private void deleteAllStockItems() {
		DbSession session = dao().getSession();
		Query query = session.createQuery("delete from stockmanagement.StockItem");
		query.executeUpdate();
	}
	
	@Test
	public void getOrderItemsByOrderByOrderIds() {
		stockManagementService.setDao(dao());
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		//Given
		OrderItem orderItem = eu().newOrderItem(dao(), stockItem, packagingUom);
		dao().saveOrderItem(orderItem);
		List<OrderItem> orderItems = stockManagementService.getOrderItemsByOrder(orderItem.getOrder().getId());
		assertNotNull(orderItems);
		assertEquals(orderItems.size(), 1);
		assertEquals(orderItems.get(0).getUuid(), orderItem.getUuid());
	}
	
	@Test
	public void getOrderItemsByEncounterByEncounterIds() {
		stockManagementService.setDao(dao());
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		//Given
		OrderItem orderItem = eu().newOrderItem(dao(), stockItem, packagingUom);
		dao().saveOrderItem(orderItem);
		List<OrderItem> orderItems = stockManagementService
		        .getOrderItemsByEncounter(orderItem.getOrder().getEncounter().getEncounterId());
		assertNotNull(orderItems);
		assertEquals(orderItems.size(), 1);
		assertEquals(orderItems.get(0).getUuid(), orderItem.getUuid());
	}
	
	private void updateOrderScheduledDate(Order order, Date scheduledDate) {
		DbSession session = dao().getSession();
		Query query = session.createQuery("Update Order set scheduledDate = :scheduledDate where orderId = :orderId");
		query.setParameter("scheduledDate", scheduledDate);
		query.setParameter("orderId", order.getOrderId());
		query.executeUpdate();
		Context.flushSession();
		Context.flushSession();
	}
	
	@Test
	public void findOrderItems() {
		stockManagementService.setDao(dao());
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		//Given
		Party party = eu().getParty(dao());
		
		OrderItem orderItem = eu().newOrderItem(dao(), stockItem, packagingUom);
		//
		orderItem.setCreatedFrom(party.getLocation());
		orderItem.setFulfilmentLocation(party.getLocation());
		orderItem.setVoided(false);
		dao().saveOrderItem(orderItem);
		
		Date scheduledDate = DateUtil.today();
		updateOrderScheduledDate(orderItem.getOrder(), scheduledDate);
		
		OrderItemSearchFilter filter = new OrderItemSearchFilter();
		filter.setLimit(10);
		filter.setStartIndex(0);
		Result<OrderItemDTO> result = stockManagementService.findOrderItems(filter, null);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		assertEquals(result.getData().get(0).getUuid(), orderItem.getUuid());
		
		filter = new OrderItemSearchFilter();
		filter.setId(orderItem.getId());
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setId(Integer.MAX_VALUE);
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setEncounterIds(Arrays.asList(orderItem.getOrder().getEncounter().getEncounterId()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setEncounterIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setEncounterUuids(Arrays.asList(orderItem.getOrder().getEncounter().getUuid()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setEncounterUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setUuid(orderItem.getUuid());
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setUuid(UUID.randomUUID().toString());
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setOrderDateMin(DateUtils.addDays(scheduledDate, 1));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setOrderDateMax(scheduledDate);
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setOrderDateMax(DateUtils.addDays(scheduledDate, -1));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setOrderIds(Arrays.asList(orderItem.getOrder().getId()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setOrderIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setOrderUuids(Arrays.asList(orderItem.getOrder().getUuid()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setOrderUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setOrderNumber(orderItem.getOrder().getOrderNumber());
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setOrderNumber(
		    orderItem.getOrder().getOrderNumber().substring(0, orderItem.getOrder().getOrderNumber().length() - 1));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setOrderNumber(UUID.randomUUID().toString());
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setPatientIds(Arrays.asList(orderItem.getOrder().getPatient().getId()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setPatientIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setStockItemIds(Arrays.asList(stockItem.getId()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setStockItemIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setStockItemUuids(Arrays.asList(orderItem.getStockItem().getUuid()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setStockItemUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setDrugIds(Arrays.asList(stockItem.getDrug().getId()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setDrugIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setDrugUuids(Arrays.asList(stockItem.getDrug().getUuid()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setDrugUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setConceptIds(Arrays.asList(stockItem.getConcept().getId()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setConceptIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setConceptUuids(Arrays.asList(stockItem.getConcept().getUuid()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setConceptUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setDrugIds(Arrays.asList(stockItem.getDrug().getId()));
		filter.setConceptUuids(Arrays.asList(stockItem.getConcept().getUuid()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setSearchEitherDrugOrConceptStockItems(true);
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setConceptUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setDrugIds(Arrays.asList(Integer.MAX_VALUE));
		filter.setConceptUuids(Arrays.asList(stockItem.getConcept().getUuid()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setConceptUuids(Arrays.asList(UUID.randomUUID().toString()));
		filter.setConceptUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setCreatedFromLocationIds(Arrays.asList(orderItem.getCreatedFrom().getId()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setCreatedFromLocationIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setCreatedFromLocationUuids(Arrays.asList(orderItem.getCreatedFrom().getUuid()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setCreatedFromLocationUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setFulfilmentLocationIds(Arrays.asList(orderItem.getFulfilmentLocation().getId()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setFulfilmentLocationIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setFulfilmentLocationUuids(Arrays.asList(orderItem.getFulfilmentLocation().getUuid()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setFulfilmentLocationUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setCreatedFromPartyUuids(Arrays.asList(party.getUuid()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setCreatedFromPartyUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setCreatedFromPartyUuids(Arrays.asList(party.getUuid()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setCreatedFromPartyUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setId(orderItem.getId());
		filter.setUuid(orderItem.getUuid());
		filter.setOrderIds(Arrays.asList(orderItem.getOrder().getId()));
		filter.setOrderNumber(orderItem.getOrder().getOrderNumber());
		filter.setPatientIds(Arrays.asList(orderItem.getOrder().getPatient().getId()));
		filter.setStockItemIds(Arrays.asList(stockItem.getId()));
		filter.setStockItemUuids(Arrays.asList(orderItem.getStockItem().getUuid()));
		filter.setDrugIds(Arrays.asList(stockItem.getDrug().getId()));
		filter.setDrugUuids(Arrays.asList(stockItem.getDrug().getUuid()));
		filter.setConceptIds(Arrays.asList(stockItem.getConcept().getId()));
		filter.setConceptUuids(Arrays.asList(stockItem.getConcept().getUuid()));
		filter.setSearchEitherDrugOrConceptStockItems(true);
		filter.setCreatedFromLocationIds(Arrays.asList(orderItem.getCreatedFrom().getId()));
		filter.setCreatedFromLocationUuids(Arrays.asList(orderItem.getCreatedFrom().getUuid()));
		filter.setFulfilmentLocationIds(Arrays.asList(orderItem.getFulfilmentLocation().getId()));
		filter.setFulfilmentLocationUuids(Arrays.asList(orderItem.getFulfilmentLocation().getUuid()));
		filter.setCreatedFromPartyUuids(Arrays.asList(party.getUuid()));
		filter.setEncounterIds(Arrays.asList(orderItem.getOrder().getEncounter().getEncounterId()));
		filter.setEncounterUuids(Arrays.asList(orderItem.getOrder().getEncounter().getUuid()));
		filter.setCreatedFromPartyUuids(Arrays.asList(party.getUuid()));
		filter.setOrderDateMin(scheduledDate);
		filter.setOrderDateMax(scheduledDate);
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setId(Integer.MAX_VALUE);
		result = stockManagementService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
	}
	
	@Test
	public void findStockRules() {
		stockManagementService.setDao(dao());
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		
		StockRule stockRule = eu().newStockRule(dao(), stockItem, packagingUom);
		stockRule.setVoided(false);
		stockRule.setEnabled(true);
		dao().saveStockRule(stockRule);
		
		StockRuleSearchFilter filter = new StockRuleSearchFilter();
		filter.setLimit(10);
		filter.setStartIndex(0);
		Result<StockRuleDTO> result = stockManagementService.findStockRules(filter, null);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		assertEquals(result.getData().get(0).getUuid(), stockRule.getUuid());
		
		filter = new StockRuleSearchFilter();
		filter.setId(stockRule.getId());
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(stockRule.getUuid())));
		
		filter.setId(Integer.MAX_VALUE);
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new StockRuleSearchFilter();
		filter.setEnabled(true);
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(stockRule.getUuid())));
		
		filter.setEnabled(false);
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new StockRuleSearchFilter();
		filter.setHasNotificationRoleSet(true);
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(stockRule.getUuid())));
		
		filter.setHasNotificationRoleSet(false);
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new StockRuleSearchFilter();
		filter.setUuids(Arrays.asList(stockRule.getUuid()));
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(stockRule.getUuid())));
		
		filter.setUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new StockRuleSearchFilter();
		filter.setStockItemUuids(Arrays.asList(stockItem.getUuid()));
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(stockRule.getUuid())));
		
		filter.setStockItemUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new StockRuleSearchFilter();
		filter.setLocationUuids(Arrays.asList(entityUtil.getLocation().getUuid()));
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(stockRule.getUuid())));
		
		filter.setLocationUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new StockRuleSearchFilter();
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(stockRule.getUuid())));
		
		filter.setLastEvaluationMin(DateUtils.addDays(stockRule.getLastEvaluation(), 1));
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new StockRuleSearchFilter();
		filter.setLastEvaluationMin(stockRule.getLastEvaluation());
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(stockRule.getUuid())));
		
		filter.setLastEvaluationMax(DateUtils.addDays(stockRule.getLastEvaluation(), -1));
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new StockRuleSearchFilter();
		filter.setLastEvaluationMax(stockRule.getLastEvaluation());
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(stockRule.getUuid())));
		
		filter = new StockRuleSearchFilter();
		filter.setNextEvaluationMin(DateUtils.addDays(stockRule.getNextEvaluation(), 1));
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new StockRuleSearchFilter();
		filter.setNextEvaluationMin(stockRule.getNextEvaluation());
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(stockRule.getUuid())));
		
		filter.setNextEvaluationMax(DateUtils.addDays(stockRule.getNextEvaluation(), -1));
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new StockRuleSearchFilter();
		filter.setNextEvaluationMax(stockRule.getNextEvaluation());
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(stockRule.getUuid())));
		
		filter = new StockRuleSearchFilter();
		filter.setLastActionDateMin(DateUtils.addDays(stockRule.getLastActionDate(), 1));
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new StockRuleSearchFilter();
		filter.setLastActionDateMin(stockRule.getLastActionDate());
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(stockRule.getUuid())));
		
		filter.setLastActionDateMax(DateUtils.addDays(stockRule.getLastActionDate(), -1));
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new StockRuleSearchFilter();
		filter.setLastActionDateMax(stockRule.getLastActionDate());
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(stockRule.getUuid())));
		
		filter = new StockRuleSearchFilter();
		filter.setId(stockRule.getId());
		filter.setUuids(Arrays.asList(stockRule.getUuid()));
		filter.setStockItemUuids(Arrays.asList(stockItem.getUuid()));
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(stockRule.getUuid())));
		
		filter.setId(Integer.MAX_VALUE);
		result = stockManagementService.findStockRules(filter, null);
		assertTrue(result.getData().isEmpty());
	}
	
	@Test
	public void findBatchJobs() {
		stockManagementService.setDao(dao());
		BatchJobDTO batchJobDTO = new BatchJobDTO();
		batchJobDTO.setBatchJobType(BatchJobType.Report);
		batchJobDTO.setDescription("Stock Receipt Report");
		StringBuilder parameters = new StringBuilder();
		String newLine = "\r\n";
		parameters.append("param.report: " + "STOCK_RECEIPT_REPORT");
		parameters.append(newLine);
		parameters.append("param.StartDate.description: Starting");
		parameters.append(newLine);
		Date date = DateUtils.addDays(new Date(), 90);
		parameters.append("param.StartDate.value.desc: " + DateUtil.formatDDMMMyyyy(date));
		parameters.append(newLine);
		parameters.append("param.StartDate.value: " + DateUtil.formatDateForJson(date));
		parameters.append(newLine);
		parameters.append("param.EndDate.description: Ending");
		parameters.append(newLine);
		date = new Date();
		parameters.append("param.EndDate.value.desc: " + DateUtil.formatDDMMMyyyy(date));
		parameters.append(newLine);
		parameters.append("param.EndDate.value: " + DateUtil.formatDateForJson(date));
		parameters.append(newLine);
		parameters.append("param.StockItemCategory.description: Stock Item Category");
		parameters.append(newLine);
		date = new Date();
		parameters.append("param.StockItemCategory.value.desc: Cough Syrup");
		parameters.append(newLine);
		parameters.append("param.StockItemCategory.value: 0cbe2ed3-cd5f-4f46-9459-26127c9265ab");
		parameters.append(newLine);
		parameters.append("param.Location.description: Location");
		parameters.append(newLine);
		date = new Date();
		parameters.append("param.Location.value.desc: Unknown Location");
		parameters.append(newLine);
		parameters.append("param.Location.value: 8d6c993e-c2cc-11de-8d13-0010c6dffd0f");
		parameters.append(newLine);
		parameters.append("param.ChildLocations.description: Include Child Locations");
		parameters.append(newLine);
		date = new Date();
		parameters.append("param.ChildLocations.value.desc: Yes");
		parameters.append(newLine);
		parameters.append("param.ChildLocations.value: true");
		parameters.append(newLine);
		batchJobDTO.setParameters(parameters.toString());
		batchJobDTO.setLocationScopeUuid("8d6c993e-c2cc-11de-8d13-0010c6dffd0f");
		batchJobDTO.setPrivilegeScope(Privileges.APP_STOCKMANAGEMENT_REPORTS);
		BatchJobDTO batchJob = stockManagementService.saveBatchJob(batchJobDTO);
		
		Location location = Context.getLocationService().getLocationByUuid("8d6c993e-c2cc-11de-8d13-0010c6dffd0f");
		
		BatchJobSearchFilter filter = new BatchJobSearchFilter();
		filter.setLimit(10);
		filter.setStartIndex(0);
		Result<BatchJobDTO> result = stockManagementService.findBatchJobs(filter, null);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		assertEquals(result.getData().get(0).getUuid(), batchJob.getUuid());
		
		filter = new BatchJobSearchFilter();
		filter.setBatchJobIds(Arrays.asList(batchJob.getId()));
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(batchJob.getUuid())));
		
		filter.setBatchJobIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new BatchJobSearchFilter();
		filter.setBatchJobUuids(Arrays.asList(batchJob.getUuid()));
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(batchJob.getUuid())));
		
		filter.setBatchJobUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new BatchJobSearchFilter();
		filter.setParameters(batchJob.getParameters());
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(batchJob.getUuid())));
		
		filter.setParameters(UUID.randomUUID().toString());
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new BatchJobSearchFilter();
		filter.setPrivilegeScope(batchJob.getPrivilegeScope());
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(batchJob.getUuid())));
		
		filter.setPrivilegeScope(UUID.randomUUID().toString());
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new BatchJobSearchFilter();
		filter.setLocationScopeIds(Arrays.asList(location.getId()));
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(batchJob.getUuid())));
		
		filter.setLocationScopeIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new BatchJobSearchFilter();
		filter.setBatchJobType(BatchJobType.Report);
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(batchJob.getUuid())));
		
		filter.setBatchJobType(BatchJobType.Other);
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new BatchJobSearchFilter();
		filter.setBatchJobStatus(Arrays.asList(BatchJobStatus.Pending));
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(batchJob.getUuid())));
		
		filter.setBatchJobStatus(Arrays.asList(BatchJobStatus.Cancelled));
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new BatchJobSearchFilter();
		filter.setDateCreatedMin(batchJob.getDateCreated());
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(batchJob.getUuid())));
		
		filter.setDateCreatedMin(DateUtils.addDays(batchJob.getDateCreated(), 1));
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new BatchJobSearchFilter();
		filter.setDateCreatedMax(batchJob.getDateCreated());
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(batchJob.getUuid())));
		
		filter.setDateCreatedMax(DateUtils.addDays(batchJob.getDateCreated(), -1));
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new BatchJobSearchFilter();
		
		filter.setBatchJobIds(Arrays.asList(batchJob.getId()));
		filter.setBatchJobUuids(Arrays.asList(batchJob.getUuid()));
		filter.setParameters(batchJob.getParameters());
		filter.setPrivilegeScope(batchJob.getPrivilegeScope());
		filter.setLocationScopeIds(Arrays.asList(location.getId()));
		filter.setBatchJobType(BatchJobType.Report);
		filter.setBatchJobStatus(Arrays.asList(BatchJobStatus.Pending));
		filter.setDateCreatedMin(batchJob.getDateCreated());
		filter.setDateCreatedMax(batchJob.getDateCreated());
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(batchJob.getUuid())));
		
		filter.setBatchJobIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockManagementService.findBatchJobs(filter, null);
		assertTrue(result.getData().isEmpty());
	}
	
	private StockSource createNonVoidedStockSource() {
		StockSource stockSource = eu().newStockSource(dao());
		stockSource.setVoided(false);
		dao().saveStockSource(stockSource);
		return stockSource;
	}
	
	private StockItemPackagingUomReference createReferenceForServiceTests() {
		stockManagementService.setDao(dao());
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setVoided(false);
		packagingUom.setFactor(BigDecimal.valueOf(30));
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		
		StockSource stockSource = createNonVoidedStockSource();
		
		StockItemPackagingUomReference reference = new StockItemPackagingUomReference();
		reference.setStockItemPackagingUOM(packagingUom);
		reference.setReferenceSource(stockSource);
		reference.setStockReferenceCode("M221680001EP30T");
		reference.setName("UNHTS packaging reference");
		reference.setDescription("UNHTS packaging reference description");
		return Context.getService(StockItemService.class).saveStockItemPackagingUomReference(reference);
	}
	
	@Test
	public void saveStockItemPackagingUomReference_shouldSaveReferenceWithAllProperties() {
		StockItemPackagingUomReference savedReference = createReferenceForServiceTests();
		assertNotNull(savedReference);
		assertNotNull(savedReference.getUuid());
		
		Context.flushSession();
		Context.clearSession();
		
		StockItemPackagingUomReference fetched = Context.getService(StockItemService.class)
		        .getStockItemPackagingUomReferenceByUuid(savedReference.getUuid());
		assertNotNull(fetched);
		assertEquals("M221680001EP30T", fetched.getStockReferenceCode());
		assertEquals("UNHTS packaging reference", fetched.getName());
		assertEquals("UNHTS packaging reference description", fetched.getDescription());
		assertNotNull(fetched.getStockItemPackagingUOM());
		assertEquals(0, fetched.getStockItemPackagingUOM().getFactor().compareTo(BigDecimal.valueOf(30)));
	}
	
	@Test
	public void saveStockItemPackagingUomReference_shouldRejectNonExistentPackagingUom() {
		stockManagementService.setDao(dao());
		StockSource stockSource = createNonVoidedStockSource();
		
		StockItemPackagingUOM nonExistentPackagingUom = new StockItemPackagingUOM();
		nonExistentPackagingUom.setUuid("non-existent-packaging-uom-uuid");
		
		StockItemPackagingUomReference reference = new StockItemPackagingUomReference();
		reference.setStockItemPackagingUOM(nonExistentPackagingUom);
		reference.setReferenceSource(stockSource);
		reference.setStockReferenceCode("M221680001EP30T");
		
		try {
			Context.getService(StockItemService.class).saveStockItemPackagingUomReference(reference);
			fail("Expected an APIException to be thrown");
		}
		catch (APIException e) {
			assertThat(e.getMessage(), containsString("packaging UOM"));
		}
	}
	
	@Test
	public void saveStockItemPackagingUomReference_shouldRejectMissingStockSource() {
		stockManagementService.setDao(dao());
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setVoided(false);
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		
		StockItemPackagingUomReference reference = new StockItemPackagingUomReference();
		reference.setStockItemPackagingUOM(packagingUom);
		reference.setStockReferenceCode("M221680001EP30T");
		
		try {
			Context.getService(StockItemService.class).saveStockItemPackagingUomReference(reference);
			fail("Expected an APIException to be thrown");
		}
		catch (APIException e) {
			assertThat(e.getMessage(), containsString("Stock source"));
		}
	}
	
	@Test
	public void saveStockItemPackagingUomReference_shouldRejectBlankReferenceCode() {
		stockManagementService.setDao(dao());
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setVoided(false);
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		
		StockSource stockSource = createNonVoidedStockSource();
		
		StockItemPackagingUomReference reference = new StockItemPackagingUomReference();
		reference.setStockItemPackagingUOM(packagingUom);
		reference.setReferenceSource(stockSource);
		reference.setStockReferenceCode("   ");
		
		try {
			Context.getService(StockItemService.class).saveStockItemPackagingUomReference(reference);
			fail("Expected an APIException to be thrown");
		}
		catch (APIException e) {
			assertThat(e.getMessage(), containsString("Stock reference code is required"));
		}
	}
	
	@Test
	public void saveStockItemPackagingUomReference_shouldRejectDuplicateActiveSourceAndCode() {
		StockItemPackagingUomReference savedReference = createReferenceForServiceTests();
		
		StockItemPackagingUomReference duplicate = new StockItemPackagingUomReference();
		duplicate.setStockItemPackagingUOM(savedReference.getStockItemPackagingUOM());
		duplicate.setReferenceSource(savedReference.getReferenceSource());
		duplicate.setStockReferenceCode("M221680001EP30T");
		
		try {
			Context.getService(StockItemService.class).saveStockItemPackagingUomReference(duplicate);
			fail("Expected an APIException to be thrown");
		}
		catch (APIException e) {
			// the search stops finding dormant references, so the message names the reference
			// that holds the code
			assertThat(e.getMessage(), containsString("already exists"));
			assertThat(e.getMessage(), containsString(savedReference.getUuid()));
		}
	}
	
	@Test
	public void saveStockItemPackagingUomReference_shouldAllowSameCodeFromDifferentSource() {
		StockItemPackagingUomReference savedReference = createReferenceForServiceTests();
		
		StockSource otherSource = createNonVoidedStockSource();
		StockItemPackagingUomReference otherNamespaceReference = new StockItemPackagingUomReference();
		otherNamespaceReference.setStockItemPackagingUOM(savedReference.getStockItemPackagingUOM());
		otherNamespaceReference.setReferenceSource(otherSource);
		otherNamespaceReference.setStockReferenceCode("M221680001EP30T");
		
		StockItemPackagingUomReference saved = Context.getService(StockItemService.class)
		        .saveStockItemPackagingUomReference(otherNamespaceReference);
		assertNotNull(saved);
		assertNotNull(saved.getUuid());
	}
	
	@Test
	public void getStockItemPackagingUomByReference_shouldResolvePackagingUomFromSourceAndCode() {
		StockItemPackagingUomReference savedReference = createReferenceForServiceTests();
		
		StockItemPackagingUOM resolved = Context.getService(StockItemService.class)
		        .getStockItemPackagingUomByReference(savedReference.getReferenceSource(), "M221680001EP30T");
		assertNotNull(resolved);
		assertEquals(0, resolved.getFactor().compareTo(BigDecimal.valueOf(30)));
	}
	
	@Test
	public void getStockItemPackagingUomByReference_shouldReturnNullWhenNoReferenceExists() {
		stockManagementService.setDao(dao());
		StockSource stockSource = createNonVoidedStockSource();
		StockItemPackagingUOM resolved = Context.getService(StockItemService.class)
		        .getStockItemPackagingUomByReference(stockSource, "UNKNOWN-CODE");
		assertNull(resolved);
	}
	
	@Test
	public void getStockItemPackagingUomReferencesBySourceAndCode_shouldReturnEmptyListForUnknownSource() {
		stockManagementService.setDao(dao());
		List<StockItemPackagingUomReference> references = Context.getService(StockItemService.class)
		        .getStockItemPackagingUomReferencesBySourceAndCode("unknown-source-uuid", "M221680001EP30T");
		assertNotNull(references);
		assertTrue(references.isEmpty());
	}
	
	@Test
	public void getStockItemPackagingUomReferencesByPackagingUom_shouldReturnReferencesOfThePackagingUom() {
		StockItemPackagingUomReference savedReference = createReferenceForServiceTests();
		
		List<StockItemPackagingUomReference> references = Context.getService(StockItemService.class)
		        .getStockItemPackagingUomReferencesByPackagingUom(savedReference.getStockItemPackagingUOM().getUuid(),
		            false);
		assertNotNull(references);
		assertEquals(1, references.size());
		assertEquals(savedReference.getUuid(), references.get(0).getUuid());
	}
	
	@Test
	public void voidStockItemPackagingUomReference_shouldVoidTheReference() {
		StockItemPackagingUomReference savedReference = createReferenceForServiceTests();
		
		Context.getService(StockItemService.class).voidStockItemPackagingUomReference(savedReference.getUuid(), "test void",
		    savedReference.getCreator().getUserId());
		Context.flushSession();
		Context.clearSession();
		
		StockItemPackagingUomReference voidedReference = Context.getService(StockItemService.class)
		        .getStockItemPackagingUomReferenceByUuid(savedReference.getUuid());
		assertNotNull(voidedReference);
		assertTrue(voidedReference.getVoided());
		assertEquals("test void", voidedReference.getVoidReason());
	}
	
	@Test
	public void voidStockItemPackagingUomReference_shouldBeANoOpWhenAlreadyVoided() {
		StockItemPackagingUomReference savedReference = createReferenceForServiceTests();
		
		Context.getService(StockItemService.class).voidStockItemPackagingUomReference(savedReference.getUuid(), "test void",
		    savedReference.getCreator().getUserId());
		Context.flushSession();
		Context.clearSession();
		
		// a second void must not append the suffix again or replace the original void reason
		Context.getService(StockItemService.class).voidStockItemPackagingUomReference(savedReference.getUuid(),
		    "second void", savedReference.getCreator().getUserId());
		Context.flushSession();
		Context.clearSession();
		
		StockItemPackagingUomReference voidedReference = Context.getService(StockItemService.class)
		        .getStockItemPackagingUomReferenceByUuid(savedReference.getUuid());
		assertTrue(voidedReference.getVoided());
		assertTrue(voidedReference.getStockReferenceCode().endsWith("(voided " + savedReference.getId() + ")")
		        && !voidedReference.getStockReferenceCode()
		                .endsWith("(voided " + savedReference.getId() + ") (voided " + savedReference.getId() + ")"));
		assertEquals("test void", voidedReference.getVoidReason());
	}
	
	@Test
	public void saveStockItemPackagingUomReference_shouldReuseACodeVoidedInTheSameTransaction() {
		StockItemPackagingUomReference reference = createReferenceForServiceTests();
		Context.getService(StockItemService.class).voidStockItemPackagingUomReference(reference.getUuid(), "moved",
		    reference.getCreator().getUserId());
		
		StockItemPackagingUomReference replacement = new StockItemPackagingUomReference();
		replacement.setStockItemPackagingUOM(reference.getStockItemPackagingUOM());
		replacement.setReferenceSource(reference.getReferenceSource());
		replacement.setStockReferenceCode("M221680001EP30T");
		assertNotNull(Context.getService(StockItemService.class).saveStockItemPackagingUomReference(replacement).getId());
	}
	
	@Test
	public void saveStockItemPackagingUomReference_shouldAllowRecreatingAfterVoid() {
		StockItemPackagingUomReference savedReference = createReferenceForServiceTests();
		
		Context.getService(StockItemService.class).voidStockItemPackagingUomReference(savedReference.getUuid(), "test void",
		    savedReference.getCreator().getUserId());
		Context.flushSession();
		Context.clearSession();
		
		// voiding releases the source/code pair, so a replacement reference can reuse it
		StockItemPackagingUomReference replacement = new StockItemPackagingUomReference();
		replacement.setStockItemPackagingUOM(savedReference.getStockItemPackagingUOM());
		replacement.setReferenceSource(savedReference.getReferenceSource());
		replacement.setStockReferenceCode("M221680001EP30T");
		StockItemPackagingUomReference recreated = Context.getService(StockItemService.class)
		        .saveStockItemPackagingUomReference(replacement);
		assertNotNull(recreated);
		assertNotNull(recreated.getUuid());
	}
	
	@Test
	public void saveStockItemPackagingUomReference_shouldRejectDuplicateCodeWhenEditingAnExistingReference() {
		StockItemPackagingUomReference firstReference = createReferenceForServiceTests();
		
		StockItemPackagingUomReference secondReference = new StockItemPackagingUomReference();
		secondReference.setStockItemPackagingUOM(firstReference.getStockItemPackagingUOM());
		secondReference.setReferenceSource(firstReference.getReferenceSource());
		secondReference.setStockReferenceCode("OTHER-CODE");
		secondReference = Context.getService(StockItemService.class).saveStockItemPackagingUomReference(secondReference);
		
		// the duplicate check must run without flushing the pending code edit, or the database
		// unique constraint rejects it before the service's "already exists" message is raised
		secondReference.setStockReferenceCode("M221680001EP30T");
		try {
			Context.getService(StockItemService.class).saveStockItemPackagingUomReference(secondReference);
			fail("Expected an APIException to be thrown");
		}
		catch (APIException e) {
			assertThat(e.getMessage(), containsString("already exists"));
		}
	}
	
	@Test
	public void saveStockItemPackagingUomReference_shouldUpdateAnExistingReference() {
		StockItemPackagingUomReference reference = createReferenceForServiceTests();
		reference.setName("renamed");
		Context.getService(StockItemService.class).saveStockItemPackagingUomReference(reference);
		Context.flushSession();
		Context.clearSession();
		
		assertEquals("renamed", Context.getService(StockItemService.class)
		        .getStockItemPackagingUomReferenceByUuid(reference.getUuid()).getName());
	}
	
	@Test
	public void saveStockItemPackagingUomReference_shouldRejectEditingAVoidedReference() {
		StockItemPackagingUomReference reference = createReferenceForServiceTests();
		
		Context.getService(StockItemService.class).voidStockItemPackagingUomReference(reference.getUuid(), "test void",
		    reference.getCreator().getUserId());
		Context.flushSession();
		Context.clearSession();
		
		StockItemPackagingUomReference voidedReference = Context.getService(StockItemService.class)
		        .getStockItemPackagingUomReferenceByUuid(reference.getUuid());
		voidedReference.setStockReferenceCode("M221680001EP30T");
		try {
			Context.getService(StockItemService.class).saveStockItemPackagingUomReference(voidedReference);
			fail("Expected an APIException to be thrown");
		}
		catch (APIException e) {
			assertThat(e.getMessage(), containsString("voided"));
		}
	}
	
	@Test
	public void saveUserRoleScope_shouldDefaultNullLocationAndOperationTypeLists() {
		stockManagementService.setDao(dao());
		UserRoleScopeDTO dto = new UserRoleScopeDTO();
		dto.setUserUuid(Context.getUserService().getUser(501).getUuid());
		dto.setRole("Provider");
		// locations and operation types deliberately left null: saving must not NPE
		UserRoleScope saved = stockManagementService.saveUserRoleScope(dto);
		assertNotNull(saved.getId());
		assertNotNull(saved.getRole());
		assertFalse(saved.getEnabled());
		assertFalse(saved.getPermanent());
	}
	
	@Test
	public void saveStockItemPackagingUomReference_shouldRejectAReferenceCodeTooLongToVoid() {
		stockManagementService.setDao(dao());
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setVoided(false);
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		StockSource stockSource = createNonVoidedStockSource();
		
		// 235 characters is the longest code voiding can still suffix within VARCHAR(255)
		char[] longestVoidableCode = new char[235];
		Arrays.fill(longestVoidableCode, 'X');
		StockItemPackagingUomReference reference = new StockItemPackagingUomReference();
		reference.setStockItemPackagingUOM(packagingUom);
		reference.setReferenceSource(stockSource);
		reference.setStockReferenceCode(new String(longestVoidableCode));
		assertNotNull(Context.getService(StockItemService.class).saveStockItemPackagingUomReference(reference).getId());
		
		// one character more and the (voided <id>) suffix would not fit
		char[] tooLongCode = new char[236];
		Arrays.fill(tooLongCode, 'X');
		StockItemPackagingUomReference tooLong = new StockItemPackagingUomReference();
		tooLong.setStockItemPackagingUOM(packagingUom);
		tooLong.setReferenceSource(stockSource);
		tooLong.setStockReferenceCode(new String(tooLongCode));
		try {
			Context.getService(StockItemService.class).saveStockItemPackagingUomReference(tooLong);
			fail("Expected an APIException to be thrown");
		}
		catch (APIException e) {
			assertThat(e.getMessage(), containsString("too long"));
		}
	}
	
	@Test
	public void voidStockItemPackagingUomReference_shouldRejectALegacyCodeTooLongForTheSuffix() {
		stockManagementService.setDao(dao());
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setVoided(false);
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		StockSource stockSource = createNonVoidedStockSource();
		
		// a row saved before the save-side length guard existed: inserted through the dao, since
		// the service would no longer accept a code the (voided <id>) suffix cannot fit
		StockItemPackagingUomReference legacy = new StockItemPackagingUomReference();
		legacy.setStockItemPackagingUOM(packagingUom);
		legacy.setReferenceSource(stockSource);
		char[] legacyCode = new char[250];
		Arrays.fill(legacyCode, 'X');
		legacy.setStockReferenceCode(new String(legacyCode));
		StockItemPackagingUomReference saved = itemDao().saveStockItemPackagingUomReference(legacy);
		
		try {
			Context.getService(StockItemService.class).voidStockItemPackagingUomReference(saved.getUuid(), "test void",
			    saved.getCreator().getUserId());
			fail("Expected an APIException to be thrown");
		}
		catch (APIException e) {
			assertThat(e.getMessage(), containsString("too long"));
		}
		
		// the failed void must not have left the managed entity half-voided (voided set, code
		// unsuffixed): re-reading after a flush shows the reference as it was
		Context.flushSession();
		Context.clearSession();
		StockItemPackagingUomReference untouched = Context.getService(StockItemService.class)
		        .getStockItemPackagingUomReferenceByUuid(saved.getUuid());
		assertFalse(untouched.getVoided());
		assertEquals(new String(legacyCode), untouched.getStockReferenceCode());
	}
	
	@Test
	public void saveStockItemPackagingUomReference_shouldRejectAPackagingUomVoidedInTheDatabase() {
		stockManagementService.setDao(dao());
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setVoided(false);
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		Context.getService(StockItemService.class).voidStockItemPackagingUOM(packagingUom.getUuid(), "test void",
		    Context.getAuthenticatedUser().getUserId());
		// the bulk-HQL void bypasses the session cache: clear it so the save path reads the row
		Context.flushSession();
		Context.clearSession();
		
		// a stale detached copy still claims non-voided: the guard must test the fetched row
		StockItemPackagingUOM staleCopy = new StockItemPackagingUOM();
		staleCopy.setId(packagingUom.getId());
		staleCopy.setUuid(packagingUom.getUuid());
		staleCopy.setVoided(false);
		
		StockSource stockSource = createNonVoidedStockSource();
		StockItemPackagingUomReference reference = new StockItemPackagingUomReference();
		reference.setStockItemPackagingUOM(staleCopy);
		reference.setReferenceSource(stockSource);
		reference.setStockReferenceCode("M221680001EP30T");
		try {
			Context.getService(StockItemService.class).saveStockItemPackagingUomReference(reference);
			fail("Expected an APIException to be thrown");
		}
		catch (APIException e) {
			assertThat(e.getMessage(), containsString("voided stock item packaging UOM"));
		}
	}
	
	@Test
	public void saveStockItemPackagingUomReference_shouldRejectAStockSourceThatDoesNotExist() {
		stockManagementService.setDao(dao());
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setVoided(false);
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		
		// a fabricated source with a valid-looking id: the uuid existence check must catch it
		// before it dies at flush on the foreign key
		StockSource fabricated = new StockSource();
		fabricated.setId(999999);
		fabricated.setUuid("non-existent-source-uuid");
		
		StockItemPackagingUomReference reference = new StockItemPackagingUomReference();
		reference.setStockItemPackagingUOM(packagingUom);
		reference.setReferenceSource(fabricated);
		reference.setStockReferenceCode("M221680001EP30T");
		try {
			Context.getService(StockItemService.class).saveStockItemPackagingUomReference(reference);
			fail("Expected an APIException to be thrown");
		}
		catch (APIException e) {
			assertThat(e.getMessage(), containsString("Stock source is required and must exist"));
		}
	}
	
	@Test
	public void getStockItemPackagingUomByReference_shouldNotResolveAReferenceOfAVoidedPackagingUom() {
		StockItemPackagingUomReference savedReference = createReferenceForServiceTests();
		Context.getService(StockItemService.class).voidStockItemPackagingUOM(
		    savedReference.getStockItemPackagingUOM().getUuid(), "test void", savedReference.getCreator().getUserId());
		Context.flushSession();
		Context.clearSession();
		
		// the reference row stays active, but its packaging UOM is voided: resolving the code
		// must not hand back a voided packaging entity
		StockItemPackagingUOM resolved = Context.getService(StockItemService.class)
		        .getStockItemPackagingUomByReference(savedReference.getReferenceSource(), "M221680001EP30T");
		assertNull(resolved);
	}
	
	@Test
	public void saveStockItemPackagingUomReference_shouldRejectAVoidedStockSource() {
		stockManagementService.setDao(dao());
		StockItem stockItem = eu().newStockItem(dao(), false);
		Context.getService(StockItemService.class).saveStockItem(stockItem);
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setVoided(false);
		Context.getService(StockItemService.class).saveStockItemPackagingUOM(packagingUom);
		
		StockSource stockSource = createNonVoidedStockSource();
		stockSource.setVoided(true);
		dao().saveStockSource(stockSource);
		Context.flushSession();
		Context.clearSession();
		
		// the caller's copy still shows the source as active: the guard must test the fetched row
		stockSource.setVoided(false);
		StockItemPackagingUomReference reference = new StockItemPackagingUomReference();
		reference.setStockItemPackagingUOM(packagingUom);
		reference.setReferenceSource(stockSource);
		reference.setStockReferenceCode("M221680001EP30T");
		try {
			Context.getService(StockItemService.class).saveStockItemPackagingUomReference(reference);
			fail("Expected an APIException to be thrown");
		}
		catch (APIException e) {
			assertThat(e.getMessage(), containsString("voided stock source"));
		}
	}
	
	@Test
	public void getStockItemPackagingUomByReference_shouldNotResolveAReferenceOfAVoidedSource() {
		StockItemPackagingUomReference savedReference = createReferenceForServiceTests();
		StockSource stockSource = savedReference.getReferenceSource();
		stockSource.setVoided(true);
		dao().saveStockSource(stockSource);
		Context.flushSession();
		Context.clearSession();
		
		// the source half of the dormancy filter: the reference row stays active, but resolving
		// its code must not hand back anything for a voided source
		StockItemPackagingUOM resolved = Context.getService(StockItemService.class)
		        .getStockItemPackagingUomByReference(stockSource, "M221680001EP30T");
		assertNull(resolved);
	}
	
	@Test
	public void getStockItemPackagingUomReferencesBySourceAndCode_shouldExcludeReferencesOfAVoidedPackagingUom() {
		StockItemPackagingUomReference savedReference = createReferenceForServiceTests();
		Context.getService(StockItemService.class).voidStockItemPackagingUOM(
		    savedReference.getStockItemPackagingUOM().getUuid(), "test void", savedReference.getCreator().getUserId());
		Context.flushSession();
		Context.clearSession();
		
		// the REST search uses the same resolvable filter as the resolve, so a client is never
		// handed a stockItemPackagingUomUuid that stockitempackaginguom answers 404 for
		List<StockItemPackagingUomReference> references = Context.getService(StockItemService.class)
		        .getStockItemPackagingUomReferencesBySourceAndCode(savedReference.getReferenceSource().getUuid(),
		            "M221680001EP30T");
		assertNotNull(references);
		assertTrue(references.isEmpty());
	}
	
	@Test
	public void saveUserRoleScope_shouldNotThrow_whenLocationEnableDescendantsIsNull() {
		stockManagementService.setDao(dao());
		UserRoleScopeDTO dto = new UserRoleScopeDTO();
		dto.setUserUuid(Context.getUserService().getUser(501).getUuid());
		dto.setRole("Provider");
		UserRoleScopeLocationDTO locationDto = new UserRoleScopeLocationDTO();
		locationDto.setLocationUuid(Context.getLocationService().getLocation(1).getUuid());
		// enableDescendants deliberately left null: adding this location must not NPE
		dto.setLocations(new ArrayList<>(Arrays.asList(locationDto)));
		
		UserRoleScope saved = stockManagementService.saveUserRoleScope(dto);
		// the point of this test is that saving with a null enableDescendants does not throw; the
		// location rows are persisted separately from the in-memory scope collection
		assertNotNull(saved.getId());
	}
	
	private StockItemDAO itemDao() {
		return Context.getRegisteredComponent("stockmanagement.StockItemDAO", StockItemDAO.class);
	}
}
