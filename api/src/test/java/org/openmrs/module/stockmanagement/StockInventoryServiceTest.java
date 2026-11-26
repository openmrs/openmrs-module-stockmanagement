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
import org.openmrs.annotation.Authorized;
import org.openmrs.module.stockmanagement.api.StockInventoryService;
import org.openmrs.module.stockmanagement.api.dao.HibernateStockInventoryDao;
import org.openmrs.module.stockmanagement.api.impl.StockInventoryServiceImpl;
import java.lang.reflect.Method;

public class StockInventoryServiceTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	DbSessionFactory sessionFactory;
	
	private StockManagementDao daoInstance;
	
	private HibernateStockInventoryDao inventoryDaoInstance;
	
	private static EntityUtil entityUtil;
	
	private StockInventoryServiceImpl stockInventoryService;
	
	private StockManagementDao dao() {
		if (daoInstance == null) {
			daoInstance = new StockManagementDao();
			daoInstance.setSessionFactory(sessionFactory);
		}
		return daoInstance;
	}
	
	private HibernateStockInventoryDao inventoryDao() {
		if (inventoryDaoInstance == null) {
			inventoryDaoInstance = new HibernateStockInventoryDao();
			inventoryDaoInstance.setSessionFactory(sessionFactory);
		}
		return inventoryDaoInstance;
	}
	
	@BeforeEach
	public void setup() throws Exception {
		initializeInMemoryDatabase();
		executeDataSet(EntityUtil.STOCK_OPERATION_TYPE_DATA_SET);
		stockInventoryService = new StockInventoryServiceImpl();
		stockInventoryService.setDao(inventoryDao());
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
	public void allServiceMethods_shouldDeclarePrivileges() {
		for (Method method : StockInventoryService.class.getDeclaredMethods()) {
			assertNotNull(method.getAnnotation(Authorized.class), method.getName());
		}
	}
	
	@Test
	public void getStockInventory_shouldFilterOnAllCriteria() {
		StockItem stockItem = eu().newStockItem(dao(), false);
		dao().saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = entityUtil.newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		dao().saveStockItemPackagingUOM(packagingUom);
		
		StockBatch stockBatch = eu().newStockBatch(dao(), stockItem);
		dao().saveStockBatch(stockBatch);
		
		StockItemTransaction so = eu().newStockItemTransaction(dao(), stockItem, stockBatch, packagingUom);
		so.setQuantity(BigDecimal.valueOf(10));
		dao().saveStockItemTransaction(so);
		
		StockItemInventorySearchFilter filter = new StockItemInventorySearchFilter();
		List<StockItemInventorySearchFilter.ItemGroupFilter> itemsToSearch = new ArrayList<>();
		StockItemInventorySearchFilter.ItemGroupFilter itemGroupFilter = new StockItemInventorySearchFilter.ItemGroupFilter(
		        Arrays.asList(so.getParty().getId()), so.getStockItem().getId(), so.getStockBatch().getId());
		itemsToSearch.add(itemGroupFilter);
		filter.setItemGroupFilters(itemsToSearch);
		Result<StockItemInventory> result = stockInventoryService.getStockInventory(filter, null);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		assertTrue(result.getData().get(0).getQuantity().setScale(2, BigDecimal.ROUND_HALF_EVEN)
		        .compareTo(so.getQuantity().multiply(BigDecimal.valueOf(2))) == 0);
		
		filter.setDoSetQuantityUoM(true);
		result = stockInventoryService.getStockInventory(filter, null);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		//assertEquals(result.getData().get(0).getQuantity().setScale(2, BigDecimal.ROUND_HALF_EVEN),so.getQuantity().setScale(2, BigDecimal.ROUND_HALF_EVEN).multiply(packagingUom.getFactor()));
		//assertTrue(result.getData().get(0).getQuantity().setScale(2, BigDecimal.ROUND_HALF_EVEN).compareTo(so.getQuantity().setScale(2, BigDecimal.ROUND_HALF_EVEN)) == 0);
	}
	
	@Test
	public void findStockItemTransactions_shouldFilterOnAllCriteria() {
		StockItemTransaction so = eu().newStockItemTransaction(dao());
		so.setQuantity(BigDecimal.TEN);
		dao().saveStockItemTransaction(so);
		
		StockItemTransactionSearchFilter filter = new StockItemTransactionSearchFilter();
		filter.setStockItemId(so.getStockItem().getId());
		Result<StockItemTransactionDTO> result = inventoryDao().findStockItemTransactions(filter, null);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		assertTrue(
		    result.getData().get(0).getQuantity().setScale(2, BigDecimal.ROUND_HALF_EVEN).compareTo(so.getQuantity()) == 0);
		
		filter = new StockItemTransactionSearchFilter();
		filter.setUuid(so.getUuid());
		result = inventoryDao().findStockItemTransactions(filter, null);
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
			result = inventoryDao().findStockItemTransactions(filter, null);
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
		dao().saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = entityUtil.newStockItemPackagingUOM(dao(), false, stockItem);
		dao().saveStockItemPackagingUOM(packagingUom);
		
		StockBatch stockBatch = eu().newStockBatch(dao(), stockItem);
		dao().saveStockBatch(stockBatch);
		
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
		
		stockInventoryService.dispenseStockItems(dispenseRequests);
		
		StockItemTransactionSearchFilter searchFilter = new StockItemTransactionSearchFilter();
		searchFilter.setPartyId(eu().getParty(dao()).getId());
		searchFilter.setStockItemId(stockItem.getId());
		
		Result<StockItemTransactionDTO> result = inventoryDao().findStockItemTransactions(searchFilter, null);
		assertThat(result.getData().size(), greaterThanOrEqualTo(4));
		List<StockItemTransactionDTO> entity = result.getData().stream()
		        .filter(p -> dispenseRequests.get(0).getPatientId().equals(p.getPatientId())).collect(Collectors.toList());
		assertEquals(entity.size(), 2);
		assertTrue(entity.stream().anyMatch(p -> dispenseRequests.stream().anyMatch(x -> p.getQuantity()
		        .multiply(BigDecimal.valueOf(-1)).setScale(2, BigDecimal.ROUND_HALF_EVEN).compareTo(x.getQuantity()) == 0)));
	}
	
	@Test
	public void getOrderItemsByOrderByOrderIds() {
		StockItem stockItem = eu().newStockItem(dao(), false);
		dao().saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		dao().saveStockItemPackagingUOM(packagingUom);
		//Given
		OrderItem orderItem = eu().newOrderItem(dao(), stockItem, packagingUom);
		inventoryDao().saveOrderItem(orderItem);
		List<OrderItem> orderItems = stockInventoryService.getOrderItemsByOrder(orderItem.getOrder().getId());
		assertNotNull(orderItems);
		assertEquals(orderItems.size(), 1);
		assertEquals(orderItems.get(0).getUuid(), orderItem.getUuid());
	}
	
	@Test
	public void getOrderItemsByEncounterByEncounterIds() {
		StockItem stockItem = eu().newStockItem(dao(), false);
		dao().saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		dao().saveStockItemPackagingUOM(packagingUom);
		//Given
		OrderItem orderItem = eu().newOrderItem(dao(), stockItem, packagingUom);
		inventoryDao().saveOrderItem(orderItem);
		List<OrderItem> orderItems = stockInventoryService
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
		StockItem stockItem = eu().newStockItem(dao(), false);
		dao().saveStockItem(stockItem);
		
		StockItemPackagingUOM packagingUom = eu().newStockItemPackagingUOM(dao(), false, stockItem);
		packagingUom.setFactor(BigDecimal.valueOf(2));
		dao().saveStockItemPackagingUOM(packagingUom);
		//Given
		Party party = eu().getParty(dao());
		
		OrderItem orderItem = eu().newOrderItem(dao(), stockItem, packagingUom);
		//
		orderItem.setCreatedFrom(party.getLocation());
		orderItem.setFulfilmentLocation(party.getLocation());
		orderItem.setVoided(false);
		inventoryDao().saveOrderItem(orderItem);
		
		Date scheduledDate = DateUtil.today();
		updateOrderScheduledDate(orderItem.getOrder(), scheduledDate);
		
		OrderItemSearchFilter filter = new OrderItemSearchFilter();
		filter.setLimit(10);
		filter.setStartIndex(0);
		Result<OrderItemDTO> result = stockInventoryService.findOrderItems(filter, null);
		assertThat(result.getData().size(), greaterThanOrEqualTo(1));
		assertEquals(result.getData().get(0).getUuid(), orderItem.getUuid());
		
		filter = new OrderItemSearchFilter();
		filter.setId(orderItem.getId());
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setId(Integer.MAX_VALUE);
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setEncounterIds(Arrays.asList(orderItem.getOrder().getEncounter().getEncounterId()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setEncounterIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setEncounterUuids(Arrays.asList(orderItem.getOrder().getEncounter().getUuid()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setEncounterUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setUuid(orderItem.getUuid());
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setUuid(UUID.randomUUID().toString());
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setOrderDateMin(DateUtils.addDays(scheduledDate, 1));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setOrderDateMax(scheduledDate);
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setOrderDateMax(DateUtils.addDays(scheduledDate, -1));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setOrderIds(Arrays.asList(orderItem.getOrder().getId()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setOrderIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setOrderUuids(Arrays.asList(orderItem.getOrder().getUuid()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setOrderUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setOrderNumber(orderItem.getOrder().getOrderNumber());
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setOrderNumber(
		    orderItem.getOrder().getOrderNumber().substring(0, orderItem.getOrder().getOrderNumber().length() - 1));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setOrderNumber(UUID.randomUUID().toString());
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setPatientIds(Arrays.asList(orderItem.getOrder().getPatient().getId()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setPatientIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setStockItemIds(Arrays.asList(stockItem.getId()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setStockItemIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setStockItemUuids(Arrays.asList(orderItem.getStockItem().getUuid()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setStockItemUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setDrugIds(Arrays.asList(stockItem.getDrug().getId()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setDrugIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setDrugUuids(Arrays.asList(stockItem.getDrug().getUuid()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setDrugUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setConceptIds(Arrays.asList(stockItem.getConcept().getId()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setConceptIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setConceptUuids(Arrays.asList(stockItem.getConcept().getUuid()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setConceptUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setDrugIds(Arrays.asList(stockItem.getDrug().getId()));
		filter.setConceptUuids(Arrays.asList(stockItem.getConcept().getUuid()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setSearchEitherDrugOrConceptStockItems(true);
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setConceptUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setDrugIds(Arrays.asList(Integer.MAX_VALUE));
		filter.setConceptUuids(Arrays.asList(stockItem.getConcept().getUuid()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setConceptUuids(Arrays.asList(UUID.randomUUID().toString()));
		filter.setConceptUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setCreatedFromLocationIds(Arrays.asList(orderItem.getCreatedFrom().getId()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setCreatedFromLocationIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setCreatedFromLocationUuids(Arrays.asList(orderItem.getCreatedFrom().getUuid()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setCreatedFromLocationUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setFulfilmentLocationIds(Arrays.asList(orderItem.getFulfilmentLocation().getId()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setFulfilmentLocationIds(Arrays.asList(Integer.MAX_VALUE));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setFulfilmentLocationUuids(Arrays.asList(orderItem.getFulfilmentLocation().getUuid()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setFulfilmentLocationUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setCreatedFromPartyUuids(Arrays.asList(party.getUuid()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setCreatedFromPartyUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
		
		filter = new OrderItemSearchFilter();
		filter.setCreatedFromPartyUuids(Arrays.asList(party.getUuid()));
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setCreatedFromPartyUuids(Arrays.asList(UUID.randomUUID().toString()));
		result = stockInventoryService.findOrderItems(filter, null);
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
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().stream().anyMatch(p -> p.getUuid().equalsIgnoreCase(orderItem.getUuid())));
		
		filter.setId(Integer.MAX_VALUE);
		result = stockInventoryService.findOrderItems(filter, null);
		assertTrue(result.getData().isEmpty());
	}
}
