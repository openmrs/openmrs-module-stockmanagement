/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api;

import org.openmrs.Location;
import org.openmrs.User;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.stockmanagement.api.dto.*;
import org.openmrs.module.stockmanagement.api.dto.reporting.*;
import org.openmrs.module.stockmanagement.api.model.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Handles stock balances, transactions, reserved stock, dispensing and order items. See
 * moduleApplicationContext.xml on how it is wired up.
 */
public interface StockInventoryService extends OpenmrsService {
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	List<PartyDTO> getCompleteStockDispensingLocationPartyList(Integer atLocationId);
	
	@Transactional
	@Authorized(Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_DISPENSE)
	void dispenseStockItems(List<DispenseRequest> dispenseRequests);
	
	@Transactional
	@Authorized(value = { Privileges.APP_STOCKMANAGEMENT_STOCKOPERATIONS,
	        Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE }, requireAll = false)
	void deleteReservedTransations(Integer stockOperationId);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	List<StockItemInventory> getStockBatchLocationInventory(List<Integer> stockBatchIds);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	StockInventoryResult getStockInventory(StockItemInventorySearchFilter filter);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	StockInventoryResult getStockInventory(StockItemInventorySearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	StockInventoryResult getStockItemInventory(StockItemInventorySearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	StockInventoryResult postProcessInventoryResult(StockItemInventorySearchFilter filter, StockInventoryResult result);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	<T extends StockItemInventory> void getStockInventory(StockItemInventorySearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters, Function<T, Boolean> consumer, Class<T> resultClass);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	Result<StockItemTransactionDTO> findStockItemTransactions(StockItemTransactionSearchFilter filter);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	StockItemTransaction getStockItemTransactionByUuid(String uuid);
	
	@Transactional
	@Authorized(Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE)
	StockItemTransaction saveStockItemTransaction(StockItemTransaction stockItemTransaction);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE)
	void validateStockInventoryAfterOperation(StockOperation stockOperation, StockOperationType stockOperationType,
	        MessageSourceService messageSourceService);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE)
	List<ReservedTransaction> calculateStockIssueAdjustments(StockOperation stockOperation,
	        StockOperationType stockOperationType, MessageSourceService messageSourceService);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_DISPENSE_QTY, requireAll = false)
	List<OrderItem> getOrderItemsByOrder(Integer... orderIds);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_DISPENSE_QTY, requireAll = false)
	List<OrderItem> getOrderItemsByEncounter(Integer... encounterIds);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_DISPENSE_QTY, requireAll = false)
	Result<OrderItemDTO> findOrderItems(OrderItemSearchFilter filter);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_DISPENSE_QTY, requireAll = false)
	Result<OrderItemDTO> findOrderItems(OrderItemSearchFilter filter, HashSet<RecordPrivilegeFilter> recordPrivilegeFilters);
	
	@Transactional
	@Authorized(value = Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_DISPENSE_QTY, requireAll = false)
	OrderItem saveOrderItem(OrderItem orderItem);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	void setStockItemCurrentBalanceWithDescendants(List<StockRuleCurrentQuantity> stockRuleCurrentQuantities);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	void setStockItemCurrentBalanceWithoutDescendants(List<StockRuleCurrentQuantity> stockRuleCurrentQuantities);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	void setStockItemInformation(List<StockItemInventory> reportStockItemInventories);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	Result<DispensingLineItem> findDispensingLineItems(DispensingLineFilter filter);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	Result<PrescriptionLineItem> findPrescriptionLineItems(PrescriptionLineFilter filter);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	Result<StockItemInventory> getLeastMovingStockInventory(StockItemInventorySearchFilter filter);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	Result<StockItemInventory> getMostMovingStockInventory(StockItemInventorySearchFilter filter);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	void getStockInventoryForecastData(StockItemInventorySearchFilter filter, Function<Object[], Boolean> consumer);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	void getStockInventoryExpiryForecastData(StockItemInventorySearchFilter filter, Function<Object[], Boolean> consumer);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE)
	Map<Integer, Boolean> checkStockBatchHasTransactionsAfterOperation(Integer stockOperationId,
	        List<Integer> stockBatchIds);
	
	@Transactional(readOnly = true)
	@Authorized(value = Privileges.APP_STOCKMANAGEMENT_STOCKITEMS, requireAll = false)
	HashSet<RecordPrivilegeFilter> getRecordPrivilegeFilters(User user, Location location,
	        StockOperationType stockOperationType, String stockManagementPrivilege);
}
