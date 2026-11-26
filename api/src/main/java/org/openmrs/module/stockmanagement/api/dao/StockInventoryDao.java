/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.dao;

import org.openmrs.Location;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.module.stockmanagement.api.dto.*;
import org.openmrs.module.stockmanagement.api.dto.Result;
import org.openmrs.module.stockmanagement.api.dto.StockInventoryResult;
import org.openmrs.module.stockmanagement.api.dto.StockItemInventory;
import org.openmrs.module.stockmanagement.api.dto.StockItemTransactionDTO;
import org.openmrs.module.stockmanagement.api.dto.reporting.*;
import org.openmrs.module.stockmanagement.api.dto.reporting.PrescriptionLineItem;
import org.openmrs.module.stockmanagement.api.model.OrderItem;
import org.openmrs.module.stockmanagement.api.model.StockBatch;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.module.stockmanagement.api.model.StockItemPackagingUOM;
import org.openmrs.module.stockmanagement.api.model.StockItemTransaction;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;
import org.openmrs.module.stockmanagement.api.model.Party;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public interface StockInventoryDao {
	
	StockItemTransaction getStockItemTransactionByUuid(String uuid);
	
	StockItemTransaction saveStockItemTransaction(StockItemTransaction stockItemTransaction);
	
	StockBatch getStockBatchByUuid(String uuid);
	
	StockItem getStockItemByUuid(String uuid);
	
	StockItemPackagingUOM getStockItemPackagingUOMByUuid(String uuid);
	
	Result<StockItemPackagingUOMDTO> findStockItemPackagingUOMs(StockItemPackagingUOMSearchFilter filter);
	
	StockOperationType getStockOperationTypeByUuid(String uuid);
	
	List<Party> getPartyListByLocations(Collection<Location> locations);
	
	List<org.openmrs.module.stockmanagement.api.dto.PrivilegeScope> getFlattenedUserRoleScopesByUser(User user,
	        Set<Role> roles, Location location, StockOperationType stockOperationType);
	
	Map<Integer, String> getPartyNames(List<Integer> partyIds);
	
	void setStockItemCurrentBalanceWithDescendants(List<StockRuleCurrentQuantity> stockRuleCurrentQuantities);
	
	void setStockItemCurrentBalanceWithoutDescendants(List<StockRuleCurrentQuantity> stockRuleCurrentQuantities);
	
	Result<StockItemInventory> getLeastMovingStockInventory(StockItemInventorySearchFilter filter);
	
	Result<StockItemInventory> getMostMovingStockInventory(StockItemInventorySearchFilter filter);
	
	void getStockInventoryExpiryForecastData(StockItemInventorySearchFilter filter, Function<Object[], Boolean> consumer);
	
	void getStockInventoryForecastData(StockItemInventorySearchFilter filter, Function<Object[], Boolean> consumer);
	
	<T extends StockItemInventory> void getStockInventory(StockItemInventorySearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters, Function<T, Boolean> consumer, Class<T> resultClass);
	
	StockInventoryResult getStockItemInventory(StockItemInventorySearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters);
	
	List<StockItemInventory> getStockBatchLocationInventory(List<Integer> stockBatchIds);
	
	void deleteReservedTransations(Integer stockOperationId);
	
	Result<StockItemTransactionDTO> findStockItemTransactions(StockItemTransactionSearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters);
	
	List<OrderItem> getOrderItemsByOrder(Integer... orderIds);
	
	List<OrderItem> getOrderItemsByEncounter(Integer... encounterIds);
	
	Result<org.openmrs.module.stockmanagement.api.dto.OrderItemDTO> findOrderItems(OrderItemSearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters);
	
	OrderItem getOrderItemByUuid(String uuid);
	
	OrderItem saveOrderItem(OrderItem orderItem);
	
	void setStockItemInformation(List<StockItemInventory> reportStockItemInventories);
	
	Result<DispensingLineItem> findDispensingLineItems(DispensingLineFilter filter);
	
	Result<PrescriptionLineItem> findPrescriptionLineItems(PrescriptionLineFilter filter);
	
	Map<Integer, Boolean> checkStockBatchHasTransactionsAfterOperation(Integer stockOperationId,
	        List<Integer> stockBatchIds);
}
