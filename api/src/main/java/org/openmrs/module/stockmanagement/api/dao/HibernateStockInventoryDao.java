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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.criterion.Order;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.type.IntegerType;
import org.openmrs.*;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.module.stockmanagement.api.dto.*;
import org.openmrs.module.stockmanagement.api.dto.reporting.*;
import org.openmrs.module.stockmanagement.api.model.*;
import org.openmrs.module.stockmanagement.api.utils.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({ "unchecked" })
public class HibernateStockInventoryDao extends DaoBase implements StockInventoryDao {
	
	public StockItemTransaction getStockItemTransactionByUuid(String uuid) {
		return (StockItemTransaction) getSession().createCriteria(StockItemTransaction.class)
		        .add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	public StockItemTransaction saveStockItemTransaction(StockItemTransaction stockItemTransaction) {
		getSession().saveOrUpdate(stockItemTransaction);
		return stockItemTransaction;
	}
	
	public StockBatch getStockBatchByUuid(String uuid) {
		return (StockBatch) getSession().createCriteria(StockBatch.class).add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	public StockOperationType getStockOperationTypeByUuid(String uuid) {
		return (StockOperationType) getSession().createCriteria(StockOperationType.class).add(Restrictions.eq("uuid", uuid))
		        .uniqueResult();
	}
	
	public List<Party> getPartyListByLocations(Collection<Location> locations) {
		if (locations == null || locations.isEmpty())
			return new ArrayList<>();
		Criteria criteria = getSession().createCriteria(Party.class);
		criteria.add(Restrictions.in("location", locations));
		return criteria.list();
	}
	
	public void setStockItemCurrentBalanceWithDescendants(List<StockRuleCurrentQuantity> stockRuleCurrentQuantities) {
		if (stockRuleCurrentQuantities == null || stockRuleCurrentQuantities.isEmpty())
			return;
		int startIndex = 0;
		int batchSize = 100;
		boolean hasMoreUpdatesToDo = true;
		do {
			StringBuilder hqlQuery = new StringBuilder(
			        "SELECT lt.parentLocationId as partyId, sit.stockItem.id as stockItemId,"
			                + "sum(sit.quantity * sipu.factor) as quantity\n"
			                + "from stockmanagement.LocationTree lt, stockmanagement.Party p, "
			                + " stockmanagement.StockItemTransaction sit join\n" + "\t sit.stockItemPackagingUOM sipu join\n"
			                + " sit.stockBatch sb where ");
			
			StringBuilder hqlFilter = new StringBuilder();
			appendFilter(hqlFilter, "lt.childLocationId = p.location.locationId");
			appendFilter(hqlFilter, "p.id = sit.party.id");
			StringBuilder itemGroupClause = new StringBuilder();
			int paramIndex = 0;
			HashMap<String, Object> parameterList = new HashMap<>();
			List<StockRuleCurrentQuantity> batch = stockRuleCurrentQuantities.stream().skip(startIndex * batchSize)
			        .limit(batchSize).collect(Collectors.toList());
			for (StockRuleCurrentQuantity stockRuleCurrentQuantity : batch) {
				appendORFilter(itemGroupClause, String.format(
				    "lt.parentLocationId = :pli%1$s and sit.stockItem.id = :sid%1$s", Integer.toString(paramIndex)));
				parameterList.putIfAbsent("pli" + Integer.toString(paramIndex), stockRuleCurrentQuantity.getLocationId());
				parameterList.putIfAbsent("sid" + Integer.toString(paramIndex), stockRuleCurrentQuantity.getStockItemId());
				paramIndex++;
			}
			if (paramIndex == 0) {
				break;
			}
			appendFilter(hqlFilter, itemGroupClause.toString());
			appendFilter(hqlFilter, "sb.expiration is null or sb.expiration > :today");
			parameterList.put("today", DateUtil.today());
			
			hqlFilter.append(" group by lt.parentLocationId, sit.stockItem.id ");
			hqlQuery.append(hqlFilter);
			
			DbSession session = getSession();
			Query query = session.createQuery(hqlQuery.toString());
			for (Map.Entry<String, Object> entry : parameterList.entrySet()) {
				query.setParameter(entry.getKey(), entry.getValue());
			}
			query = query.setResultTransformer(new AliasToBeanResultTransformer(StockItemInventory.class));
			List<StockItemInventory> result = query.list();
			for (StockRuleCurrentQuantity stockRuleCurrentQuantity : batch) {
				Optional<StockItemInventory> inventory = result.stream()
				        .filter(p -> p.getPartyId().equals(stockRuleCurrentQuantity.getLocationId())
				                && p.getStockItemId().equals(stockRuleCurrentQuantity.getStockItemId()))
				        .findAny();
				if (inventory.isPresent()) {
					stockRuleCurrentQuantity.setQuantity(inventory.get().getQuantity());
				}
			}
			
			hasMoreUpdatesToDo = batch.size() >= batchSize;
			startIndex++;
		} while (hasMoreUpdatesToDo);
	}
	
	public void setStockItemCurrentBalanceWithoutDescendants(List<StockRuleCurrentQuantity> stockRuleCurrentQuantities) {
		if (stockRuleCurrentQuantities == null || stockRuleCurrentQuantities.isEmpty())
			return;
		int startIndex = 0;
		boolean hasMoreUpdatesToDo = true;
		Map<Integer, Integer> locationPartyIds = getLocationPartyIds(
		    stockRuleCurrentQuantities.stream().map(p -> p.getLocationId()).distinct().collect(Collectors.toList()));
		do {
			StringBuilder hqlQuery = new StringBuilder("SELECT sit.party.id as partyId, sit.stockItem.id as stockItemId,"
			        + "sum(sit.quantity * sipu.factor) as quantity\n"
			        + "from stockmanagement.StockItemTransaction sit join\n" + "\t sit.stockItemPackagingUOM sipu join\n"
			        + " sit.stockBatch sb where ");
			
			StringBuilder hqlFilter = new StringBuilder();
			appendFilter(hqlFilter, "p.id = sit.party.id");
			StringBuilder itemGroupClause = new StringBuilder();
			int paramIndex = 0;
			HashMap<String, Object> parameterList = new HashMap<>();
			List<StockRuleCurrentQuantity> batch = stockRuleCurrentQuantities.stream().skip(startIndex * 100).limit(100)
			        .collect(Collectors.toList());
			for (StockRuleCurrentQuantity stockRuleCurrentQuantity : batch) {
				Integer partyId = locationPartyIds.getOrDefault(stockRuleCurrentQuantity.getLocationId(), null);
				if (partyId == null)
					continue;
				appendORFilter(itemGroupClause,
				    String.format("sit.party.id = :pli%1$s and sit.stockItem.id = :sid%1$s", Integer.toString(paramIndex)));
				parameterList.putIfAbsent("pli" + Integer.toString(paramIndex), partyId);
				parameterList.putIfAbsent("sid" + Integer.toString(paramIndex), stockRuleCurrentQuantity.getStockItemId());
				paramIndex++;
			}
			if (paramIndex == 0) {
				break;
			}
			
			appendFilter(hqlFilter, itemGroupClause.toString());
			appendFilter(hqlFilter, "sb.expiration is null or sb.expiration > :today");
			parameterList.put("today", DateUtil.today());
			
			hqlFilter.append(" group by sit.party.id as partyId, sit.stockItem.id");
			hqlQuery.append(hqlFilter);
			
			DbSession session = getSession();
			Query query = session.createQuery(hqlQuery.toString());
			for (Map.Entry<String, Object> entry : parameterList.entrySet()) {
				query.setParameter(entry.getKey(), entry.getValue());
			}
			query = query.setResultTransformer(new AliasToBeanResultTransformer(StockItemInventory.class));
			List<StockItemInventory> result = query.list();
			for (StockRuleCurrentQuantity stockRuleCurrentQuantity : batch) {
				Integer partyId = locationPartyIds.getOrDefault(stockRuleCurrentQuantity.getLocationId(), null);
				if (partyId == null)
					continue;
				Optional<StockItemInventory> inventory = result.stream().filter(p -> p.getPartyId().equals(partyId)
				        && p.getStockItemId().equals(stockRuleCurrentQuantity.getStockItemId())).findAny();
				if (inventory.isPresent()) {
					stockRuleCurrentQuantity.setQuantity(inventory.get().getQuantity());
				}
			}
			
			hasMoreUpdatesToDo = batch.size() >= 100;
			startIndex++;
		} while (hasMoreUpdatesToDo);
	}
	
	public Result<StockItemInventory> getLeastMovingStockInventory(StockItemInventorySearchFilter filter) {
		return getMostLeastMovingStockInventory(filter, false);
	}
	
	public Result<StockItemInventory> getMostMovingStockInventory(StockItemInventorySearchFilter filter) {
		return getMostLeastMovingStockInventory(filter, true);
	}
	
	private Result<StockItemInventory> getMostLeastMovingStockInventory(StockItemInventorySearchFilter filter,
	        boolean isMostMoving) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		List<StockItemInventorySearchFilter.ItemGroupFilter> zeroStockQtyToReturn = new ArrayList<>();
		
		if (filter.isRequireItemGroupFilters()
		        && (filter.getItemGroupFilters() == null || filter.getItemGroupFilters().isEmpty()))
			return new Result(new ArrayList<>(), 0);
		
		if (filter.getInventoryGroupBy() == null) {
			filter.setInventoryGroupBy(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo);
		}
		
		boolean groupByParty = true;
		boolean groupByStockBatch = true;
		boolean groupByStockItem = true;
		
		if (filter.getInventoryGroupBy() != null) {
			groupByParty = filter.getInventoryGroupBy()
			        .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItem)
			        || filter.getInventoryGroupBy()
			                .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo);
			
			groupByStockItem = filter.getInventoryGroupBy()
			        .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItem)
			        || filter.getInventoryGroupBy()
			                .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo)
			        || filter.getInventoryGroupBy().equals(StockItemInventorySearchFilter.InventoryGroupBy.StockItemOnly);
			
			groupByStockBatch = filter.getInventoryGroupBy()
			        .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo);
		}
		if (groupByParty == false && groupByStockBatch == false && groupByStockItem == false) {
			groupByParty = true;
			groupByStockBatch = true;
			groupByStockItem = true;
		}
		
		StringBuilder hqlQuery = getStockItemInventoryQuery(filter, null, parameterList, parameterWithList,
		    zeroStockQtyToReturn, groupByParty, groupByStockBatch, groupByStockItem);
		if (hqlQuery == null) {
			return new Result(new ArrayList<>(), 0);
		}
		hqlQuery.append(" order by quantity " + (isMostMoving ? "asc" : "desc"));
		
		org.hibernate.StatelessSession session = null;
		try {
			session = getStatelessHibernateSession();
			Query query = session.createQuery(hqlQuery.toString());
			if (parameterList != null) {
				for (Map.Entry<String, Object> entry : parameterList.entrySet())
					query.setParameter(entry.getKey(), entry.getValue());
			}
			if (parameterWithList != null) {
				for (Map.Entry<String, Collection> entry : parameterWithList.entrySet())
					query.setParameterList(entry.getKey(), entry.getValue());
			}
			query.setResultTransformer(new AliasToBeanResultTransformer(StockItemInventory.class));
			query.setReadOnly(true);
			Integer limit = filter.getLimit() == null ? 20 : filter.getLimit();
			query.setMaxResults(limit);
			query.setFetchSize(limit);
			query.setFirstResult(0);
			Result<StockItemInventory> result = new Result<>();
			result.setData(query.list());
			return result;
		}
		finally {
			if (session != null) {
				try {
					session.close();
				}
				catch (Exception e) {}
			}
		}
	}
	
	public void getStockInventoryExpiryForecastData(StockItemInventorySearchFilter filter,
	        Function<Object[], Boolean> consumer) {
		if (consumer == null)
			return;
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		List<StockItemInventorySearchFilter.ItemGroupFilter> zeroStockQtyToReturn = new ArrayList<>();
		
		if (filter.isRequireItemGroupFilters()
		        && (filter.getItemGroupFilters() == null || filter.getItemGroupFilters().isEmpty()))
			return;
		
		if (filter.getInventoryGroupBy() == null) {
			filter.setInventoryGroupBy(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo);
		}
		
		StringBuilder hqlQuery = new StringBuilder(
		        "SELECT sit.stockItem.id as stockItemId, sit.stockBatch.id as stockBatchId, sb.expiration as expiration,\n");
		LocalDate startDate = filter.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate endDate = filter.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		Integer monthQuantityConsumedIndex = 0;
		startDate = startDate.withDayOfMonth(1);
		endDate = endDate.withDayOfMonth(1);
		do {
			hqlQuery.append(
			    "sum(case when (sit.quantity < 0 and sit.dateCreated >= :startdate and sit.dateCreated <= :enddate and year(sit.dateCreated)="
			            + Integer.toString(startDate.getYear()) + " and month(sit.dateCreated)="
			            + Integer.toString(startDate.getMonthValue())
			            + " ) then (sit.quantity * sipu.factor * -1) else 0 end) as q"
			            + Integer.toString(monthQuantityConsumedIndex) + ",\n");
			startDate = startDate.plusMonths(1);
			monthQuantityConsumedIndex++;
		} while (!startDate.isAfter(endDate));
		
		hqlQuery.append(
		    "sum(case when (sb.expiration is null or sb.expiration > :today) then (sit.quantity * sipu.factor) else 0 end) as quantity");
		hqlQuery.append(" from stockmanagement.StockItemTransaction sit join\n" + "\t sit.stockItemPackagingUOM sipu join\n"
		        + " sit.stockBatch sb\n" + (filter.getStockItemCategoryConceptId() != null ? " join sit.stockItem si" : ""));
		
		StringBuilder hqlFilter = new StringBuilder();
		
		List<String> partyIdsToFetch = filter.getItemGroupFilters() == null ? new ArrayList<>()
		        : filter.getItemGroupFilters().stream()
		                .filter(p -> p.getPartyUuids() != null && !p.getPartyUuids().isEmpty()).map(p -> p.getPartyUuids())
		                .flatMap(Collection::stream).distinct().collect(Collectors.toList());
		
		List<String> stockItemIdsToFetch = filter.getItemGroupFilters() == null ? new ArrayList<>()
		        : filter.getItemGroupFilters().stream().filter(p -> p.getStockItemUuid() != null)
		                .map(p -> p.getStockItemUuid()).distinct().collect(Collectors.toList());
		
		Map<String, Integer> partyIds = partyIdsToFetch.isEmpty() ? new HashMap<>() : getPartyIds(partyIdsToFetch);
		Map<String, Integer> stockItemIds = stockItemIdsToFetch.isEmpty() ? new HashMap<>()
		        : getStockItemIds(stockItemIdsToFetch);
		StringBuilder itemGroupFilters = new StringBuilder();
		int paramIndex = 0;
		int appliedItemGroupFilters = 0;
		if (filter.getItemGroupFilters() != null) {
			for (StockItemInventorySearchFilter.ItemGroupFilter itemGroupFilter : filter.getItemGroupFilters()) {
				StringBuilder itemGroupClause = new StringBuilder();
				HashMap<String, Object> itemGroupParameterList = new HashMap<>();
				HashMap<String, Collection> itemGroupParameterWithList = new HashMap<>();
				StockItemInventorySearchFilter.ItemGroupFilter zeroQtyItemGroupFilter = new StockItemInventorySearchFilter.ItemGroupFilter();
				
				List<Integer> partyIdFilter = new ArrayList<>();
				if (itemGroupFilter.getPartyIds() != null) {
					partyIdFilter.addAll(itemGroupFilter.getPartyIds());
				}
				if (itemGroupFilter.getPartyUuids() != null) {
					List<Integer> foundPartyIds = itemGroupFilter.getPartyUuids().stream()
					        .map(p -> partyIds.getOrDefault(p, 0)).filter(p -> !p.equals(0)).collect(Collectors.toList());
					if (foundPartyIds.isEmpty()) {
						continue;
					}
					partyIdFilter.addAll(foundPartyIds);
				}
				String paramIndexString = Integer.toString(paramIndex);
				if (!partyIdFilter.isEmpty()) {
					appendFilter(itemGroupClause, String.format("sit.party.id in (:pids%1s)", paramIndexString));
					itemGroupParameterWithList.putIfAbsent("pids" + paramIndexString, partyIdFilter);
					zeroQtyItemGroupFilter.setPartyIds(partyIdFilter);
					
				}
				
				if (itemGroupFilter.getStockItemId() != null) {
					appendFilter(itemGroupClause, String.format("sit.stockItem.id = :sid%1s", paramIndexString));
					itemGroupParameterList.putIfAbsent("sid" + paramIndexString, itemGroupFilter.getStockItemId());
					zeroQtyItemGroupFilter.setStockItemId(itemGroupFilter.getStockItemId());
				} else if (!StringUtils.isBlank(itemGroupFilter.getStockItemUuid())) {
					if (!stockItemIds.containsKey(itemGroupFilter.getStockItemUuid())) {
						continue;
					}
					appendFilter(itemGroupClause, String.format("sit.stockItem.id = :sid%1s", paramIndexString));
					itemGroupParameterList.putIfAbsent("sid" + paramIndexString,
					    stockItemIds.get(itemGroupFilter.getStockItemUuid()));
					zeroQtyItemGroupFilter.setStockItemId(stockItemIds.get(itemGroupFilter.getStockItemUuid()));
				}
				
				if (itemGroupFilter.getStockBatchIds() != null && !itemGroupFilter.getStockBatchIds().isEmpty()) {
					appendFilter(itemGroupClause, String.format("sit.stockBatch.id in (:sbid%1s)", paramIndexString));
					itemGroupParameterWithList.putIfAbsent("sbid" + paramIndexString, itemGroupFilter.getStockBatchIds());
				}
				
				paramIndex++;
				if (itemGroupClause.length() > 0) {
					appliedItemGroupFilters++;
					appendORFilter(itemGroupFilters, itemGroupClause.toString());
					zeroStockQtyToReturn.add(zeroQtyItemGroupFilter);
					for (Map.Entry<String, Object> entry : itemGroupParameterList.entrySet()) {
						parameterList.putIfAbsent(entry.getKey(), entry.getValue());
					}
					for (Map.Entry<String, Collection> entry : itemGroupParameterWithList.entrySet()) {
						parameterWithList.putIfAbsent(entry.getKey(), entry.getValue());
					}
				} else {
					continue;
				}
			}
		}
		if (filter.isRequireItemGroupFilters() && appliedItemGroupFilters == 0) {
			return;
		}
		
		if (filter.getUnRestrictedPartyIds() != null && !filter.getUnRestrictedPartyIds().isEmpty()) {
			if (filter.getUnRestrictedPartyIds().size() == 1) {
				appendFilter(hqlFilter, "sit.party.id = :urpis");
				parameterList.putIfAbsent("urpis", filter.getUnRestrictedPartyIds().get(0));
			} else {
				appendFilter(hqlFilter, "sit.party.id in (:urpis)");
				parameterWithList.putIfAbsent("urpis", filter.getUnRestrictedPartyIds());
			}
		}
		
		if (itemGroupFilters.length() > 0) {
			appendFilter(hqlFilter, itemGroupFilters.toString());
		}
		
		if (filter.getStockItemCategoryConceptId() != null) {
			appendFilter(hqlFilter, "si.category.conceptId = :stockItemCategoryId");
			parameterList.putIfAbsent("stockItemCategoryId", filter.getStockItemCategoryConceptId());
		}
		
		appendFilter(hqlFilter, "sb.expiration > :startdate");
		
		parameterList.put("startdate", filter.getStartDate());
		parameterList.put("enddate", filter.getEndDate());
		parameterList.put("today", DateUtil.today());
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		hqlQuery.append(" group by sit.stockItem.id,  sit.stockBatch.id, sb.expiration");
		hqlQuery.append(" order by sit.stockItem.id, sb.expiration");
		
		ScrollableResults results = null;
		org.hibernate.StatelessSession session = null;
		try {
			session = getStatelessHibernateSession();
			Query query = session.createQuery(hqlQuery.toString());
			
			for (Map.Entry<String, Object> entry : parameterList.entrySet())
				query.setParameter(entry.getKey(), entry.getValue());
			
			for (Map.Entry<String, Collection> entry : parameterWithList.entrySet())
				query.setParameterList(entry.getKey(), entry.getValue());
			
			query.setReadOnly(true);
			query.setFetchSize(Integer.MIN_VALUE);
			results = query.scroll(ScrollMode.FORWARD_ONLY);
			while (results.next()) {
				Object[] row = results.get();
				Boolean result = consumer.apply(row);
				if (result == null || result == false) {
					break;
				}
			}
		}
		finally {
			if (results != null) {
				try {
					results.close();
				}
				catch (Exception e) {}
			}
			if (session != null) {
				try {
					session.close();
				}
				catch (Exception e) {}
			}
		}
	}
	
	public void getStockInventoryForecastData(StockItemInventorySearchFilter filter, Function<Object[], Boolean> consumer) {
		if (consumer == null)
			return;
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		List<StockItemInventorySearchFilter.ItemGroupFilter> zeroStockQtyToReturn = new ArrayList<>();
		
		if (filter.isRequireItemGroupFilters()
		        && (filter.getItemGroupFilters() == null || filter.getItemGroupFilters().isEmpty()))
			return;
		
		if (filter.getInventoryGroupBy() == null) {
			filter.setInventoryGroupBy(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo);
		}
		
		boolean groupByParty = true;
		boolean groupByStockBatch = true;
		boolean groupByStockItem = true;
		
		if (filter.getInventoryGroupBy() != null) {
			groupByParty = filter.getInventoryGroupBy()
			        .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItem)
			        || filter.getInventoryGroupBy()
			                .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo);
			
			groupByStockItem = filter.getInventoryGroupBy()
			        .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItem)
			        || filter.getInventoryGroupBy()
			                .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo)
			        || filter.getInventoryGroupBy().equals(StockItemInventorySearchFilter.InventoryGroupBy.StockItemOnly);
			
			groupByStockBatch = filter.getInventoryGroupBy()
			        .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo);
		}
		if (groupByParty == false && groupByStockBatch == false && groupByStockItem == false) {
			groupByParty = true;
			groupByStockBatch = true;
			groupByStockItem = true;
		}
		
		StringBuilder hqlQuery = new StringBuilder("SELECT "
		        + (groupByParty ? "sit.party.id as partyId,\n" : "cast(:nullValue as java.lang.Integer) as partyId,")
		        + (groupByStockItem ? "sit.stockItem.id as stockItemId,\n"
		                : "cast(:nullValue as java.lang.Integer) as stockItemId,")
		        + (groupByStockBatch ? "sit.stockBatch.id as stockBatchId,\n"
		                : "cast(:nullValue as java.lang.Integer) as stockBatchId,"));
		
		LocalDate startDate = filter.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate endDate = filter.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		Integer monthQuantityConsumedIndex = 0;
		startDate = startDate.withDayOfMonth(1);
		endDate = endDate.withDayOfMonth(1);
		do {
			hqlQuery.append(
			    "sum(case when (sit.quantity < 0 and sit.dateCreated >= :startdate and sit.dateCreated <= :enddate and year(sit.dateCreated)="
			            + Integer.toString(startDate.getYear()) + " and month(sit.dateCreated)="
			            + Integer.toString(startDate.getMonthValue())
			            + " ) then (sit.quantity * sipu.factor * -1) else 0 end) as q"
			            + Integer.toString(monthQuantityConsumedIndex) + ",");
			startDate = startDate.plusMonths(1);
			monthQuantityConsumedIndex++;
		} while (!startDate.isAfter(endDate));
		
		hqlQuery.append(
		    "sum(case when (sb.expiration is null or sb.expiration > :today) then (sit.quantity * sipu.factor) else 0 end) as quantity");
		hqlQuery.append(" from stockmanagement.StockItemTransaction sit join\n" + "\t sit.stockItemPackagingUOM sipu join\n"
		        + " sit.stockBatch sb\n" + (filter.getStockItemCategoryConceptId() != null ? " join sit.stockItem si" : ""));
		
		StringBuilder hqlFilter = new StringBuilder();
		
		List<String> partyIdsToFetch = filter.getItemGroupFilters() == null ? new ArrayList<>()
		        : filter.getItemGroupFilters().stream()
		                .filter(p -> p.getPartyUuids() != null && !p.getPartyUuids().isEmpty()).map(p -> p.getPartyUuids())
		                .flatMap(Collection::stream).distinct().collect(Collectors.toList());
		
		List<String> stockItemIdsToFetch = filter.getItemGroupFilters() == null ? new ArrayList<>()
		        : filter.getItemGroupFilters().stream().filter(p -> p.getStockItemUuid() != null)
		                .map(p -> p.getStockItemUuid()).distinct().collect(Collectors.toList());
		
		Map<String, Integer> partyIds = partyIdsToFetch.isEmpty() ? new HashMap<>() : getPartyIds(partyIdsToFetch);
		Map<String, Integer> stockItemIds = stockItemIdsToFetch.isEmpty() ? new HashMap<>()
		        : getStockItemIds(stockItemIdsToFetch);
		StringBuilder itemGroupFilters = new StringBuilder();
		int paramIndex = 0;
		int appliedItemGroupFilters = 0;
		if (filter.getItemGroupFilters() != null) {
			for (StockItemInventorySearchFilter.ItemGroupFilter itemGroupFilter : filter.getItemGroupFilters()) {
				StringBuilder itemGroupClause = new StringBuilder();
				HashMap<String, Object> itemGroupParameterList = new HashMap<>();
				HashMap<String, Collection> itemGroupParameterWithList = new HashMap<>();
				StockItemInventorySearchFilter.ItemGroupFilter zeroQtyItemGroupFilter = new StockItemInventorySearchFilter.ItemGroupFilter();
				
				List<Integer> partyIdFilter = new ArrayList<>();
				if (itemGroupFilter.getPartyIds() != null) {
					partyIdFilter.addAll(itemGroupFilter.getPartyIds());
				}
				if (itemGroupFilter.getPartyUuids() != null) {
					List<Integer> foundPartyIds = itemGroupFilter.getPartyUuids().stream()
					        .map(p -> partyIds.getOrDefault(p, 0)).filter(p -> !p.equals(0)).collect(Collectors.toList());
					if (foundPartyIds.isEmpty()) {
						continue;
					}
					partyIdFilter.addAll(foundPartyIds);
				}
				String paramIndexString = Integer.toString(paramIndex);
				if (!partyIdFilter.isEmpty()) {
					appendFilter(itemGroupClause, String.format("sit.party.id in (:pids%1s)", paramIndexString));
					itemGroupParameterWithList.putIfAbsent("pids" + paramIndexString, partyIdFilter);
					zeroQtyItemGroupFilter.setPartyIds(partyIdFilter);
					
				}
				
				if (itemGroupFilter.getStockItemId() != null) {
					appendFilter(itemGroupClause, String.format("sit.stockItem.id = :sid%1s", paramIndexString));
					itemGroupParameterList.putIfAbsent("sid" + paramIndexString, itemGroupFilter.getStockItemId());
					zeroQtyItemGroupFilter.setStockItemId(itemGroupFilter.getStockItemId());
				} else if (!StringUtils.isBlank(itemGroupFilter.getStockItemUuid())) {
					if (!stockItemIds.containsKey(itemGroupFilter.getStockItemUuid())) {
						continue;
					}
					appendFilter(itemGroupClause, String.format("sit.stockItem.id = :sid%1s", paramIndexString));
					itemGroupParameterList.putIfAbsent("sid" + paramIndexString,
					    stockItemIds.get(itemGroupFilter.getStockItemUuid()));
					zeroQtyItemGroupFilter.setStockItemId(stockItemIds.get(itemGroupFilter.getStockItemUuid()));
				}
				
				if (itemGroupFilter.getStockBatchIds() != null && !itemGroupFilter.getStockBatchIds().isEmpty()) {
					appendFilter(itemGroupClause, String.format("sit.stockBatch.id in (:sbid%1s)", paramIndexString));
					itemGroupParameterWithList.putIfAbsent("sbid" + paramIndexString, itemGroupFilter.getStockBatchIds());
				}
				
				paramIndex++;
				if (itemGroupClause.length() > 0) {
					appliedItemGroupFilters++;
					appendORFilter(itemGroupFilters, itemGroupClause.toString());
					zeroStockQtyToReturn.add(zeroQtyItemGroupFilter);
					for (Map.Entry<String, Object> entry : itemGroupParameterList.entrySet()) {
						parameterList.putIfAbsent(entry.getKey(), entry.getValue());
					}
					for (Map.Entry<String, Collection> entry : itemGroupParameterWithList.entrySet()) {
						parameterWithList.putIfAbsent(entry.getKey(), entry.getValue());
					}
				} else {
					continue;
				}
			}
		}
		if (filter.isRequireItemGroupFilters() && appliedItemGroupFilters == 0) {
			return;
		}
		
		if (filter.getUnRestrictedPartyIds() != null && !filter.getUnRestrictedPartyIds().isEmpty()) {
			if (filter.getUnRestrictedPartyIds().size() == 1) {
				appendFilter(hqlFilter, "sit.party.id = :urpis");
				parameterList.putIfAbsent("urpis", filter.getUnRestrictedPartyIds().get(0));
			} else {
				appendFilter(hqlFilter, "sit.party.id in (:urpis)");
				parameterWithList.putIfAbsent("urpis", filter.getUnRestrictedPartyIds());
			}
		}
		
		if (itemGroupFilters.length() > 0) {
			appendFilter(hqlFilter, itemGroupFilters.toString());
		}
		
		if (filter.getStockItemCategoryConceptId() != null) {
			appendFilter(hqlFilter, "si.category.conceptId = :stockItemCategoryId");
			parameterList.putIfAbsent("stockItemCategoryId", filter.getStockItemCategoryConceptId());
		}
		
		appendFilter(hqlFilter, "sb.expiration is null or sb.expiration > :startdate");
		
		parameterList.put("startdate", filter.getStartDate());
		parameterList.put("enddate", filter.getEndDate());
		parameterList.put("today", DateUtil.today());
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		hqlQuery.append(" group by ");
		List<String> groupColumns = new ArrayList<>();
		if (groupByParty) {
			groupColumns.add("sit.party.id");
		}
		if (groupByStockItem) {
			groupColumns.add("sit.stockItem.id");
		}
		if (groupByStockBatch) {
			groupColumns.add("sit.stockBatch.id");
		}
		hqlQuery.append(String.join(", ", groupColumns));
		
		if (hqlQuery == null) {
			return;
		}
		hqlQuery.append(" order by " + (groupByParty ? "sit.party.id"
		        : (groupByStockItem ? "sit.stockItem.id" : (groupByStockBatch ? "sit.stockBatch.id" : ""))));
		
		ScrollableResults results = null;
		org.hibernate.StatelessSession session = null;
		try {
			session = getStatelessHibernateSession();
			Query query = session.createQuery(hqlQuery.toString());
			if (!groupByParty || !groupByStockBatch || !groupByStockItem) {
				query.setParameter("nullValue", null, IntegerType.INSTANCE);
			}
			
			for (Map.Entry<String, Object> entry : parameterList.entrySet())
				query.setParameter(entry.getKey(), entry.getValue());
			
			for (Map.Entry<String, Collection> entry : parameterWithList.entrySet())
				query.setParameterList(entry.getKey(), entry.getValue());
			
			query.setReadOnly(true);
			query.setFetchSize(Integer.MIN_VALUE);
			results = query.scroll(ScrollMode.FORWARD_ONLY);
			while (results.next()) {
				Object[] row = results.get();
				Boolean result = consumer.apply(row);
				if (result == null || result == false) {
					break;
				}
			}
		}
		finally {
			if (results != null) {
				try {
					results.close();
				}
				catch (Exception e) {}
			}
			if (session != null) {
				try {
					session.close();
				}
				catch (Exception e) {}
			}
		}
	}
	
	public <T extends StockItemInventory> void getStockInventory(StockItemInventorySearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters, Function<T, Boolean> consumer, Class<T> resultClass) {
		if (consumer == null)
			return;
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		List<StockItemInventorySearchFilter.ItemGroupFilter> zeroStockQtyToReturn = new ArrayList<>();
		
		if (filter.isRequireItemGroupFilters()
		        && (filter.getItemGroupFilters() == null || filter.getItemGroupFilters().isEmpty()))
			return;
		
		if (filter.getInventoryGroupBy() == null) {
			filter.setInventoryGroupBy(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo);
		}
		
		boolean groupByParty = true;
		boolean groupByStockBatch = true;
		boolean groupByStockItem = true;
		
		if (filter.getInventoryGroupBy() != null) {
			groupByParty = filter.getInventoryGroupBy()
			        .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItem)
			        || filter.getInventoryGroupBy()
			                .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo);
			
			groupByStockItem = filter.getInventoryGroupBy()
			        .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItem)
			        || filter.getInventoryGroupBy()
			                .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo)
			        || filter.getInventoryGroupBy().equals(StockItemInventorySearchFilter.InventoryGroupBy.StockItemOnly);
			
			groupByStockBatch = filter.getInventoryGroupBy()
			        .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo);
		}
		if (groupByParty == false && groupByStockBatch == false && groupByStockItem == false) {
			groupByParty = true;
			groupByStockBatch = true;
			groupByStockItem = true;
		}
		
		StringBuilder hqlQuery = getStockItemInventoryQuery(filter, recordPrivilegeFilters, parameterList, parameterWithList,
		    zeroStockQtyToReturn, groupByParty, groupByStockBatch, groupByStockItem);
		if (hqlQuery == null) {
			return;
		}
		hqlQuery.append(" order by " + (groupByParty ? "sit.party.id"
		        : (groupByStockItem ? "sit.stockItem.id" : (groupByStockBatch ? "sit.stockBatch.id" : ""))));
		
		ScrollableResults results = null;
		org.hibernate.StatelessSession session = null;
		try {
			session = getStatelessHibernateSession();
			Query query = session.createQuery(hqlQuery.toString());
			if (parameterList != null) {
				for (Map.Entry<String, Object> entry : parameterList.entrySet())
					query.setParameter(entry.getKey(), entry.getValue());
			}
			if (parameterWithList != null) {
				for (Map.Entry<String, Collection> entry : parameterWithList.entrySet())
					query.setParameterList(entry.getKey(), entry.getValue());
			}
			query.setResultTransformer(new AliasToBeanResultTransformer(resultClass));
			query.setReadOnly(true);
			query.setFetchSize(Integer.MIN_VALUE);
			results = query.scroll(ScrollMode.FORWARD_ONLY);
			while (results.next()) {
				T stockItemInventory = (T) results.get(0);
				Boolean result = consumer.apply(stockItemInventory);
				if (result == null || result == false) {
					break;
				}
			}
		}
		finally {
			if (results != null) {
				try {
					results.close();
				}
				catch (Exception e) {}
			}
			if (session != null) {
				try {
					session.close();
				}
				catch (Exception e) {}
			}
		}
	}
	
	public StockInventoryResult getStockItemInventory(StockItemInventorySearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		List<StockItemInventorySearchFilter.ItemGroupFilter> zeroStockQtyToReturn = new ArrayList<>();
		
		if (filter.isRequireItemGroupFilters()
		        && (filter.getItemGroupFilters() == null || filter.getItemGroupFilters().isEmpty()))
			return new StockInventoryResult(new ArrayList<>(), 0);
		
		if (filter.getInventoryGroupBy() == null) {
			filter.setInventoryGroupBy(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo);
		}
		
		boolean groupByParty = true;
		boolean groupByStockBatch = true;
		boolean groupByStockItem = true;
		
		if (filter.getInventoryGroupBy() != null) {
			groupByParty = filter.getInventoryGroupBy()
			        .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItem)
			        || filter.getInventoryGroupBy()
			                .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo);
			
			groupByStockItem = filter.getInventoryGroupBy()
			        .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItem)
			        || filter.getInventoryGroupBy()
			                .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo)
			        || filter.getInventoryGroupBy().equals(StockItemInventorySearchFilter.InventoryGroupBy.StockItemOnly);
			
			groupByStockBatch = filter.getInventoryGroupBy()
			        .equals(StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo);
		}
		if (groupByParty == false && groupByStockBatch == false && groupByStockItem == false) {
			groupByParty = true;
			groupByStockBatch = true;
			groupByStockItem = true;
		}
		
		StringBuilder hqlQuery = getStockItemInventoryQuery(filter, recordPrivilegeFilters, parameterList, parameterWithList,
		    zeroStockQtyToReturn, groupByParty, groupByStockBatch, groupByStockItem);
		if (hqlQuery == null) {
			return new StockInventoryResult(new ArrayList<>(), 0);
		}
		
		StockInventoryResult result = new StockInventoryResult();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		String orderByField = (groupByParty ? "sit.party.id"
		        : (groupByStockItem ? "sit.stockItem.id" : (groupByStockBatch ? "sit.stockBatch.id" : "")));
		result.setData(executeQuery(StockItemInventory.class, hqlQuery, result,
		    filter.getLimit() != null ? " order by " + orderByField : null, parameterList, parameterWithList));
		
		if ((!groupByStockBatch || filter.getAllowEmptyBatchInfo()) && filter.dispensing()) {
			for (StockItemInventorySearchFilter.ItemGroupFilter itemGroupFilter : zeroStockQtyToReturn) {
				if (groupByStockItem) {
					if (groupByParty) {
						for (Integer partyId : itemGroupFilter.getPartyIds()) {
							if (!result.getData().stream()
							        .anyMatch(p -> p.getStockItemId().equals(itemGroupFilter.getStockItemId())
							                && p.getPartyId().equals(partyId))) {
								if (groupByStockBatch && filter.getAllowEmptyBatchInfo()
								        && filter.getEmptyBatchPartyId() != null
								        && !filter.getEmptyBatchPartyId().equals(partyId)) {
									continue;
								}
								StockItemInventory stockItemInventory = new StockItemInventory();
								stockItemInventory.setStockItemId(itemGroupFilter.getStockItemId());
								stockItemInventory.setPartyId(partyId);
								stockItemInventory.setQuantity(BigDecimal.ZERO);
								result.getData().add(stockItemInventory);
							}
						}
					} else {
						if (!result.getData().stream()
						        .anyMatch(p -> p.getStockItemId().equals(itemGroupFilter.getStockItemId()))) {
							StockItemInventory stockItemInventory = new StockItemInventory();
							stockItemInventory.setStockItemId(itemGroupFilter.getStockItemId());
							stockItemInventory.setQuantity(BigDecimal.ZERO);
							result.getData().add(stockItemInventory);
						}
					}
				}
			}
		}
		
		if (!result.getData().isEmpty()) {
			PartySearchFilter partySearchFilter = new PartySearchFilter();
			partySearchFilter.setIncludeVoided(true);
			partySearchFilter
			        .setPartyIds(result.getData().stream().map(p -> p.getPartyId()).distinct().collect(Collectors.toList()));
			Map<Integer, List<PartyDTO>> partyDTOs = findParty(partySearchFilter).getData().stream()
			        .collect(Collectors.groupingBy(PartyDTO::getId));
			for (StockItemInventory stockItemInventory : result.getData()) {
				if (partyDTOs.containsKey(stockItemInventory.getPartyId())) {
					stockItemInventory
					        .setLocationUuid(partyDTOs.get(stockItemInventory.getPartyId()).get(0).getLocationUuid());
					stockItemInventory.setPartyUuid(partyDTOs.get(stockItemInventory.getPartyId()).get(0).getUuid());
				}
			}
			
			if (filter.includingStrength() || filter.includingConceptRefIds()) {
				List<Integer> inventoryStockItemIds = result.getData().stream().map(p -> p.getStockItemId())
				        .collect(Collectors.toList());
				Map<Integer, List<StockItemDTO>> stockItemConceptRefIds = getStockItemConceptRefs(inventoryStockItemIds)
				        .getData().stream().collect(Collectors.groupingBy(StockItemDTO::getId));
				if (!stockItemConceptRefIds.isEmpty()) {
					for (StockItemInventory stockItemInventory : result.getData()) {
						List<StockItemDTO> refs = stockItemConceptRefIds.getOrDefault(stockItemInventory.getStockItemId(),
						    null);
						if (refs != null) {
							if (filter.includingStrength()) {
								stockItemInventory.setDrugStrength(refs.get(0).getDrugStrength());
							}
							if (filter.includingConceptRefIds()) {
								stockItemInventory.setDrugId(refs.get(0).getDrugId());
								stockItemInventory.setDrugUuid(refs.get(0).getDrugUuid());
								stockItemInventory.setConceptId(refs.get(0).getConceptId());
								stockItemInventory.setConceptUuid(refs.get(0).getConceptUuid());
							}
						}
					}
				}
			}
			
			if (filter.isIncludeStockItemName()) {
				StockItemSearchFilter itemFilter = new StockItemSearchFilter();
				itemFilter.setStockItemIds(
				    result.getData().stream().map(p -> p.getStockItemId()).collect(Collectors.toList()));
				Result<StockItemDTO> stockItemDTOResult = findStockItems(itemFilter);
				if (!stockItemDTOResult.getData().isEmpty()) {
					for (StockItemInventory stockItemInventory : result.getData()) {
						Optional<StockItemDTO> stockItemDTO = stockItemDTOResult.getData().stream()
						        .filter(p -> p.getId().equals(stockItemInventory.getStockItemId())).findFirst();
						if (stockItemDTO.isPresent()) {
							stockItemInventory.setDrugName(stockItemDTO.get().getDrugName());
							stockItemInventory.setConceptName(stockItemDTO.get().getConceptName());
							stockItemInventory.setCommonName(stockItemDTO.get().getCommonName());
							stockItemInventory.setAcronym(stockItemDTO.get().getAcronym());
							stockItemInventory.setStockItemUuid(stockItemDTO.get().getUuid());
						}
					}
				}
			}
		}
		
		return result;
	}
	
	private StringBuilder getStockItemInventoryQuery(StockItemInventorySearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters, HashMap<String, Object> parameterList,
	        HashMap<String, Collection> parameterWithList,
	        List<StockItemInventorySearchFilter.ItemGroupFilter> zeroStockQtyToReturn, boolean groupByParty,
	        boolean groupByStockBatch, boolean groupByStockItem) {
		StringBuilder hqlQuery = new StringBuilder("SELECT " + (groupByParty ? "sit.party.id as partyId,\n" : "")
		        + (groupByStockItem ? "sit.stockItem.id as stockItemId,\n" : "")
		        + (groupByStockBatch ? "sit.stockBatch.id as stockBatchId,\n" : "")
		        + (filter.getInventoryMode() == null
		                || (filter.getInventoryMode().equals(StockItemInventorySearchFilter.InventoryMode.Total)
		                        || filter.getInventoryMode().equals(StockItemInventorySearchFilter.InventoryMode.MostMoving)
		                        || filter.getInventoryMode()
		                                .equals(StockItemInventorySearchFilter.InventoryMode.LeastMoving))
		                                        ? "sum(sit.quantity * sipu.factor) as quantity\n"
		                                        : "")
		        + (filter.getInventoryMode() != null && filter.getInventoryMode().equals(
		            StockItemInventorySearchFilter.InventoryMode.Consumption)
		                    ? ("sum(case when sit.dateCreated < :startdate then (sit.quantity * sipu.factor) else 0 end) as quantity, "
		                            + "sum(case when sit.dateCreated <= :enddate then (sit.quantity * sipu.factor) else 0 end) as closingQuantity, "
		                            + "sum(case when sit.quantity < 0 and  sit.dateCreated >= :startdate and sit.dateCreated <= :enddate  then (sit.quantity * sipu.factor * -1) else 0 end) as quantityConsumed,"
		                            + "sum(case when sit.quantity > 0 and  sit.dateCreated >= :startdate and sit.dateCreated <= :enddate  then (sit.quantity * sipu.factor) else 0 end) as quantityReceived")
		                    : "")
		        + " from stockmanagement.StockItemTransaction sit join\n" + "\t sit.stockItemPackagingUOM sipu join\n"
		        + " sit.stockBatch sb\n" + (filter.getStockItemCategoryConceptId() != null ? " join sit.stockItem si" : "")
		        + " \n");
		
		StringBuilder hqlFilter = new StringBuilder();
		
		List<String> partyIdsToFetch = filter.getItemGroupFilters() == null ? new ArrayList<>()
		        : filter.getItemGroupFilters().stream()
		                .filter(p -> p.getPartyUuids() != null && !p.getPartyUuids().isEmpty()).map(p -> p.getPartyUuids())
		                .flatMap(Collection::stream).distinct().collect(Collectors.toList());
		
		List<Integer> toRestrictedPartyIds = null;
		if (recordPrivilegeFilters != null) {
			PartySearchFilter partySearchFilter = new PartySearchFilter();
			partySearchFilter.setIncludeVoided(true);
			partySearchFilter.setLocationIds(
			    recordPrivilegeFilters.stream().map(p -> p.getLocationId()).distinct().collect(Collectors.toList()));
			toRestrictedPartyIds = findParty(partySearchFilter).getData().stream().map(p -> p.getId())
			        .collect(Collectors.toList());
		}
		final List<Integer> restrictedPartyIds = toRestrictedPartyIds;
		
		List<String> stockItemIdsToFetch = filter.getItemGroupFilters() == null ? new ArrayList<>()
		        : filter.getItemGroupFilters().stream().filter(p -> p.getStockItemUuid() != null)
		                .map(p -> p.getStockItemUuid()).distinct().collect(Collectors.toList());
		
		Map<String, Integer> partyIds = partyIdsToFetch.isEmpty() ? new HashMap<>() : getPartyIds(partyIdsToFetch);
		Map<String, Integer> stockItemIds = stockItemIdsToFetch.isEmpty() ? new HashMap<>()
		        : getStockItemIds(stockItemIdsToFetch);
		StringBuilder itemGroupFilters = new StringBuilder();
		int paramIndex = 0;
		int appliedItemGroupFilters = 0;
		if (filter.getItemGroupFilters() != null) {
			for (StockItemInventorySearchFilter.ItemGroupFilter itemGroupFilter : filter.getItemGroupFilters()) {
				StringBuilder itemGroupClause = new StringBuilder();
				HashMap<String, Object> itemGroupParameterList = new HashMap<>();
				HashMap<String, Collection> itemGroupParameterWithList = new HashMap<>();
				StockItemInventorySearchFilter.ItemGroupFilter zeroQtyItemGroupFilter = new StockItemInventorySearchFilter.ItemGroupFilter();
				
				List<Integer> partyIdFilter = new ArrayList<>();
				if (itemGroupFilter.getPartyIds() != null) {
					partyIdFilter.addAll(itemGroupFilter.getPartyIds());
				}
				if (itemGroupFilter.getPartyUuids() != null) {
					List<Integer> foundPartyIds = itemGroupFilter.getPartyUuids().stream()
					        .map(p -> partyIds.getOrDefault(p, 0)).filter(p -> !p.equals(0)).collect(Collectors.toList());
					if (foundPartyIds.isEmpty()) {
						// return new StockInventoryResult(new ArrayList<>(), 0);
						continue;
					}
					partyIdFilter.addAll(foundPartyIds);
				}
				String paramIndexString = Integer.toString(paramIndex);
				if (!partyIdFilter.isEmpty()) {
					if (restrictedPartyIds != null) {
						partyIdFilter = partyIdFilter.stream().filter(p -> restrictedPartyIds.contains(p))
						        .collect(Collectors.toList());
						if (partyIdFilter.isEmpty()) {
							//partyIdFilter = restrictedPartyIds;
							continue;
						}
					}
					
					appendFilter(itemGroupClause, String.format("sit.party.id in (:pids%1s)", paramIndexString));
					itemGroupParameterWithList.putIfAbsent("pids" + paramIndexString, partyIdFilter);
					zeroQtyItemGroupFilter.setPartyIds(partyIdFilter);
					
				} else if (restrictedPartyIds != null) {
					appendFilter(itemGroupClause, String.format("sit.party.id in (:pids%1s)", paramIndexString));
					itemGroupParameterWithList.putIfAbsent("pids" + paramIndexString, restrictedPartyIds);
					zeroQtyItemGroupFilter.setPartyIds(restrictedPartyIds);
				}
				
				if (itemGroupFilter.getStockItemId() != null) {
					appendFilter(itemGroupClause, String.format("sit.stockItem.id = :sid%1s", paramIndexString));
					itemGroupParameterList.putIfAbsent("sid" + paramIndexString, itemGroupFilter.getStockItemId());
					zeroQtyItemGroupFilter.setStockItemId(itemGroupFilter.getStockItemId());
				} else if (!StringUtils.isBlank(itemGroupFilter.getStockItemUuid())) {
					if (!stockItemIds.containsKey(itemGroupFilter.getStockItemUuid())) {
						// return new StockInventoryResult(new ArrayList<>(), 0);
						continue;
					}
					appendFilter(itemGroupClause, String.format("sit.stockItem.id = :sid%1s", paramIndexString));
					itemGroupParameterList.putIfAbsent("sid" + paramIndexString,
					    stockItemIds.get(itemGroupFilter.getStockItemUuid()));
					zeroQtyItemGroupFilter.setStockItemId(stockItemIds.get(itemGroupFilter.getStockItemUuid()));
				}
				
				if (itemGroupFilter.getStockBatchIds() != null && !itemGroupFilter.getStockBatchIds().isEmpty()) {
					appendFilter(itemGroupClause, String.format("sit.stockBatch.id in (:sbid%1s)", paramIndexString));
					itemGroupParameterWithList.putIfAbsent("sbid" + paramIndexString, itemGroupFilter.getStockBatchIds());
				}
				
				paramIndex++;
				if (itemGroupClause.length() > 0) {
					appliedItemGroupFilters++;
					appendORFilter(itemGroupFilters, itemGroupClause.toString());
					zeroStockQtyToReturn.add(zeroQtyItemGroupFilter);
					for (Map.Entry<String, Object> entry : itemGroupParameterList.entrySet()) {
						parameterList.putIfAbsent(entry.getKey(), entry.getValue());
					}
					for (Map.Entry<String, Collection> entry : itemGroupParameterWithList.entrySet()) {
						parameterWithList.putIfAbsent(entry.getKey(), entry.getValue());
					}
				} else {
					continue;
				}
			}
		}
		if (filter.isRequireItemGroupFilters() && appliedItemGroupFilters == 0) {
			return null;
		}
		
		if (filter.getUnRestrictedPartyIds() != null && !filter.getUnRestrictedPartyIds().isEmpty()) {
			if (filter.getUnRestrictedPartyIds().size() == 1) {
				appendFilter(hqlFilter, "sit.party.id = :urpis");
				parameterList.putIfAbsent("urpis", filter.getUnRestrictedPartyIds().get(0));
			} else {
				appendFilter(hqlFilter, "sit.party.id in (:urpis)");
				parameterWithList.putIfAbsent("urpis", filter.getUnRestrictedPartyIds());
			}
		}
		
		if (itemGroupFilters.length() > 0) {
			appendFilter(hqlFilter, itemGroupFilters.toString());
		}
		
		if (filter.getRequireNonExpiredStockBatches() != null && filter.getRequireNonExpiredStockBatches()) {
			appendFilter(hqlFilter, "sb.expiration is null or sb.expiration > :today");
			parameterList.put("today", filter.getDate() != null ? filter.getDate() : DateUtil.today());
		}
		
		if (filter.getDate() != null && (filter.getInventoryMode() == null
		        || filter.getInventoryMode().equals(StockItemInventorySearchFilter.InventoryMode.Total))) {
			appendFilter(hqlFilter, "sit.dateCreated <= :tdate");
			parameterList.put("tdate", filter.getDate());
		}
		
		if (filter.getInventoryMode() != null
		        && filter.getInventoryMode().equals(StockItemInventorySearchFilter.InventoryMode.Consumption)) {
			parameterList.put("startdate", filter.getStartDate());
			parameterList.put("enddate", filter.getEndDate());
			appendFilter(hqlFilter, "sit.dateCreated <= :enddate");
		} else if (filter.getInventoryMode() != null
		        && (filter.getInventoryMode().equals(StockItemInventorySearchFilter.InventoryMode.MostMoving)
		                || filter.getInventoryMode().equals(StockItemInventorySearchFilter.InventoryMode.LeastMoving))) {
			parameterList.put("startdate", filter.getStartDate());
			parameterList.put("enddate", filter.getEndDate());
			appendFilter(hqlFilter, "sit.dateCreated >= :startdate");
			appendFilter(hqlFilter, "sit.dateCreated <= :enddate");
		}
		
		if (filter.getStockItemCategoryConceptId() != null) {
			appendFilter(hqlFilter, "si.category.conceptId = :stockItemCategoryId");
			parameterList.putIfAbsent("stockItemCategoryId", filter.getStockItemCategoryConceptId());
		}
		
		if (filter.getInventoryMode() != null
		        && (filter.getInventoryMode().equals(StockItemInventorySearchFilter.InventoryMode.MostMoving)
		                || filter.getInventoryMode().equals(StockItemInventorySearchFilter.InventoryMode.LeastMoving))) {
			appendFilter(hqlFilter, "sit.patient.id is not null");
		}
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		hqlQuery.append(" group by ");
		List<String> groupColumns = new ArrayList<>();
		if (groupByParty) {
			groupColumns.add("sit.party.id");
		}
		if (groupByStockItem) {
			groupColumns.add("sit.stockItem.id");
		}
		if (groupByStockBatch) {
			groupColumns.add("sit.stockBatch.id");
		}
		hqlQuery.append(String.join(", ", groupColumns));
		return hqlQuery;
	}
	
	public List<StockItemInventory> getStockBatchLocationInventory(List<Integer> stockBatchIds) {
		if (stockBatchIds == null || stockBatchIds.isEmpty())
			return new ArrayList<>();
		StringBuilder hqlQuery = new StringBuilder(
		        "SELECT  sit.party.id as partyId, sit.stockItem.id as stockItemId, sit.stockBatch.id as stockBatchId, sum(sit.quantity * sipu.factor) as quantity\n"
		                + "from stockmanagement.StockItemTransaction sit join\n" + "\t sit.stockItemPackagingUOM sipu join\n"
		                + " sit.stockBatch sb where sit.stockBatch.id in (:bids) and "
		                + "(sb.expiration is null or sb.expiration > :today)\n"
		                + " group by sit.party.id, sit.stockItem.id, sit.stockBatch.id");
		
		DbSession dbSession = getSession();
		Query query = dbSession.createQuery(hqlQuery.toString());
		query.setParameterList("bids", stockBatchIds);
		query.setParameter("today", DateUtil.today());
		query = query.setResultTransformer(new AliasToBeanResultTransformer(StockItemInventory.class));
		return query.list();
	}
	
	public void deleteReservedTransations(Integer stockOperationId) {
		DbSession session = getSession();
		Query query = session.createQuery("DELETE stockmanagement.ReservedTransaction WHERE stockOperation.id = :id");
		query.setParameter("id", stockOperationId);
		query.executeUpdate();
	}
	
	public Result<StockItemTransactionDTO> findStockItemTransactions(StockItemTransactionSearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("SELECT sit.dateCreated as dateCreated,\n" + "sit.uuid as uuid,\n"
		        + "sit.party.id as partyId,\n" + "p.uuid as partyUuid,\n" + "sit.stockItem.id as stockItemId,\n"
		        + "si.uuid as stockItemUuid,\n" + "sipu.uuid as stockItemPackagingUOMUuid,\n"
		        + "sipu.packagingUom.conceptId as packagingUoMId,\n" + "sb.uuid as stockBatchUuid,\n"
		        + "sb.batchNo as stockBatchNo,\n" + "sb.expiration as expiration,\n" + "so.uuid as stockOperationUuid,\n"
		        + "so.status as stockOperationStatus,\n" + "so.operationNumber as stockOperationNumber,\n"
		        + "sot.name as stockOperationTypeName,\n" + "sit.quantity as quantity,"
		        + "sit.patient.id as patientId, sit.order.orderId as orderId, sit.encounter.encounterId as encounterId,\n"
		        + "so.source.id as operationSourcePartyId, so.destination.id as operationDestinationPartyId\n"
		        + "from stockmanagement.StockItemTransaction sit join\n"
		        + "\t sit.stockItem si left join sit.party p left join\n" + " sit.stockBatch sb left join\n"
		        + " sit.stockItemPackagingUOM sipu left join\n"
		        + " sit.stockOperation so left join so.stockOperationType sot");
		
		StringBuilder hqlFilter = new StringBuilder();
		
		if (filter.getUuid() != null) {
			appendFilter(hqlFilter, "sit.uuid = :uuid");
			parameterList.putIfAbsent("uuid", filter.getUuid());
		}
		
		if (filter.getPartyId() != null) {
			appendFilter(hqlFilter, "sit.party.id = :partyId");
			parameterList.putIfAbsent("partyId", filter.getPartyId());
		}
		
		List<PartyDTO> partyNames = null;
		if (recordPrivilegeFilters != null) {
			PartySearchFilter partySearchFilter = new PartySearchFilter();
			partySearchFilter.setIncludeVoided(true);
			partySearchFilter.setLocationIds(
			    recordPrivilegeFilters.stream().map(p -> p.getLocationId()).distinct().collect(Collectors.toList()));
			partyNames = findParty(partySearchFilter).getData().stream().collect(Collectors.toList());
			List<Integer> partyIds = partyNames.stream().map(p -> p.getId()).collect(Collectors.toList());
			if (filter.getPartyId() != null && !partyIds.contains(filter.getPartyId())) {
				return new Result<>(new ArrayList<>(), 0);
			}
			appendFilter(hqlFilter, "sit.party.id in (:permpartyIds)");
			parameterWithList.putIfAbsent("permpartyIds", partyIds);
		}
		
		if (filter.getStockOperationId() != null) {
			appendFilter(hqlFilter, "sit.stockOperation.id = :soid");
			parameterList.putIfAbsent("soid", filter.getStockOperationId());
		}
		
		if (filter.getStockItemId() != null) {
			appendFilter(hqlFilter, "sit.stockItem.id = :stockItemId");
			parameterList.putIfAbsent("stockItemId", filter.getStockItemId());
		}
		
		if (filter.getTransactionDateMin() != null) {
			appendFilter(hqlFilter, "sit.dateCreated >= :odm");
			parameterList.putIfAbsent("odm", filter.getTransactionDateMin());
		}
		
		if (filter.getTransactionDateMax() != null) {
			appendFilter(hqlFilter, "sit.dateCreated <= :odmx");
			parameterList.putIfAbsent("odmx", filter.getTransactionDateMax());
		}
		
		if (filter.getIsPatientTransaction() != null) {
			if (filter.getIsPatientTransaction() == true) {
				appendFilter(hqlFilter, "sit.patient.id is not null");
			} else {
				appendFilter(hqlFilter, "sit.patient.id is null");
			}
		}
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		Result<StockItemTransactionDTO> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		System.out.println("STOCK: executing query: " + hqlQuery);
		result.setData(executeQuery(StockItemTransactionDTO.class, hqlQuery, result, " order by sit.id desc", parameterList,
		    parameterWithList));
		
		if (!result.getData().isEmpty()) {
			List<Integer> conceptIds = new ArrayList<>();
			conceptIds.addAll(result.getData().stream().filter(p -> p.getPackagingUoMId() != null)
			        .map(p -> p.getPackagingUoMId()).collect(Collectors.toList()));
			List<ConceptNameDTO> conceptNameDTOs = conceptIds.isEmpty() ? new ArrayList<>()
			        : getConceptNamesByConceptIds(conceptIds);
			
			PartySearchFilter partySearchFilter = new PartySearchFilter();
			partySearchFilter.setIncludeVoided(true);
			List<Integer> namePartyIds = result.getData().stream()
			        .map(p -> Arrays.asList(p.getPartyId(), p.getOperationSourcePartyId(),
			            p.getOperationDestinationPartyId()))
			        .flatMap(Collection::stream).filter(p -> p != null).distinct().collect(Collectors.toList());
			namePartyIds.addAll(namePartyIds);
			partySearchFilter.setPartyIds(namePartyIds);
			Map<Integer, List<PartyDTO>> partyNameMap = findParty(partySearchFilter).getData().stream()
			        .collect(Collectors.groupingBy(PartyDTO::getId));
			
			for (StockItemTransactionDTO stockItemTransactionDTO : result.getData()) {
				
				if (stockItemTransactionDTO.getPackagingUoMId() != null
				        && stockItemTransactionDTO.getStockItemPackagingUOMUuid() != null) {
					Optional<ConceptNameDTO> conceptNameDTO = conceptNameDTOs.stream()
					        .filter(p -> p.getConceptId().equals(stockItemTransactionDTO.getPackagingUoMId())).findFirst();
					if (conceptNameDTO.isPresent()) {
						stockItemTransactionDTO.setPackagingUomName(conceptNameDTO.get().getName());
					}
					
					BigDecimal factor = getStockItemPackagingUOMByUuid(
					    stockItemTransactionDTO.getStockItemPackagingUOMUuid()).getFactor();
					
					stockItemTransactionDTO.setPackagingUomFactor(factor);
				}
				
				if (stockItemTransactionDTO.getPartyId() != null) {
					List<PartyDTO> party = partyNameMap.getOrDefault(stockItemTransactionDTO.getPartyId(), null);
					if (party != null) {
						stockItemTransactionDTO.setPartyName(party.get(0).getName());
					}
				}
				
				if (stockItemTransactionDTO.getOperationSourcePartyId() != null) {
					List<PartyDTO> party = partyNameMap.getOrDefault(stockItemTransactionDTO.getOperationSourcePartyId(),
					    null);
					if (party != null) {
						stockItemTransactionDTO.setOperationSourcePartyName(party.get(0).getName());
					}
				}
				
				if (stockItemTransactionDTO.getOperationDestinationPartyId() != null) {
					List<PartyDTO> party = partyNameMap
					        .getOrDefault(stockItemTransactionDTO.getOperationDestinationPartyId(), null);
					if (party != null) {
						stockItemTransactionDTO.setOperationDestinationPartyName(party.get(0).getName());
					}
				}
				// stockItemTransactionDTO.setPatientId(1000);
				Integer patientId = stockItemTransactionDTO.getPatientId();
				System.out.println("Got patient id as: " + patientId);
				if (patientId != null) {
					Patient patient = Context.getPatientService().getPatient(patientId);
					String patientUuid = patient.getUuid();
					System.out.println("Got patient uuid as: " + patientUuid);
					stockItemTransactionDTO.setPatientUuid(patientUuid);
				}
			}
			
		}
		return result;
	}
	
	public List<OrderItem> getOrderItemsByOrder(Integer... orderIds) {
		List<Integer> params = Arrays.asList(orderIds);
		return getSession().createCriteria(OrderItem.class).add(Restrictions.in("order.orderId", params)).list();
	}
	
	public List<OrderItem> getOrderItemsByEncounter(Integer... encounterIds) {
		List<Integer> params = Arrays.asList(encounterIds);
		Criteria criteria = getSession().createCriteria(OrderItem.class);
		criteria.createAlias("order", "o");
		criteria.createAlias("o.encounter", "e");
		
		return criteria.add(Restrictions.in("e.encounterId", params)).list();
	}
	
	public Result<OrderItemDTO> findOrderItems(OrderItemSearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("select oi.uuid as uuid, oi.id as id,\n" + "o.orderId as orderId,\n"
		        + "o.uuid as orderUuid,\n" + "o.action as action,\n"
		        + "o.orderNumber as orderNumber,o.scheduledDate as scheduledDate,\n" + "o.patient.patientId as patientId,\n"
		        + "e.uuid as encounterUuid,\n" + "si.uuid as stockItemUuid," + "si.commonName as commonName,\n"
		        + "si.acronym as acronym,\n" + "si.id as stockItemId,\n" + "si.drug.drugId as drugId,\n"
		        + "d.uuid as drugUuid,\n" + "d.name as drugName,\n" + "c.conceptId as conceptId,\n"
		        + "c.uuid as conceptUuid,\n" + "sipu.id as stockItemPackagingUOMId,\n"
		        + "sipu.uuid as stockItemPackagingUOMUuid,\n" + "pu.conceptId as stockItemPackagingUOMConceptId,\n"
		        + "oi.createdFrom.locationId as createdFrom,\n" + "cf.name as createdFromName,\n"
		        + "cf.uuid as createdFromUuid,\n" + "oi.fulfilmentLocation.locationId as fulfilmentLocationId,\n"
		        + "fl.name as fulfilmentLocationName,\n" + "fl.uuid as fulfilmentLocationUuid,\n"
		        + "oi.creator.userId as creator,\n" + "oi.dateCreated as dateCreated,\n" + "oi.voided as voided\n"
		        + "from stockmanagement.OrderItem oi left join oi.stockItem si left join\n"
		        + " si.drug d left join si.concept c left join\n" + "\t oi.order o left join o.encounter e left join\n"
		        + "\t oi.stockItemPackagingUOM sipu left join sipu.packagingUom pu left join oi.createdFrom cf left join \n"
		        + "\t oi.fulfilmentLocation fl left join o.patient p");
		
		StringBuilder hqlFilter = new StringBuilder();
		
		if (filter.getId() != null) {
			appendFilter(hqlFilter, "oi.id = :id");
			parameterList.put("id", filter.getId());
		}
		
		if (isNotNullOrEmpty(filter.getUuid())) {
			appendFilter(hqlFilter, "oi.uuid = :uuid");
			parameterList.put("uuid", filter.getUuid());
		}
		
		if (filter.getOrderIds() != null && !filter.getOrderIds().isEmpty()) {
			appendFilter(hqlFilter, "oi.order.orderId in (:orderIds)");
			parameterWithList.put("orderIds", filter.getOrderIds());
		}
		
		List<Integer> createdFromLocationIds = null;
		if (filter.getCreatedFromLocationIds() != null && !filter.getCreatedFromLocationIds().isEmpty()) {
			createdFromLocationIds = createdFromLocationIds == null ? new ArrayList<>() : createdFromLocationIds;
			createdFromLocationIds.addAll(filter.getCreatedFromLocationIds());
		}
		if (filter.getCreatedFromLocationUuids() != null && !filter.getCreatedFromLocationUuids().isEmpty()) {
			PartySearchFilter partySearchFilter = new PartySearchFilter();
			partySearchFilter.setLocationUuids(filter.getCreatedFromLocationUuids());
			createdFromLocationIds = createdFromLocationIds == null ? new ArrayList<>() : createdFromLocationIds;
			List<Integer> data = findParty(partySearchFilter).getData().stream().map(p -> p.getLocationId())
			        .collect(Collectors.toList());
			if (data.isEmpty())
				return new Result<>(new ArrayList<>(), 0);
			createdFromLocationIds.addAll(data);
		}
		if (filter.getCreatedFromPartyUuids() != null && !filter.getCreatedFromPartyUuids().isEmpty()) {
			PartySearchFilter partySearchFilter = new PartySearchFilter();
			partySearchFilter.setPartyUuids(filter.getCreatedFromPartyUuids());
			createdFromLocationIds = createdFromLocationIds == null ? new ArrayList<>() : createdFromLocationIds;
			List<Integer> data = findParty(partySearchFilter).getData().stream().map(p -> p.getLocationId())
			        .collect(Collectors.toList());
			if (data.isEmpty())
				return new Result<>(new ArrayList<>(), 0);
			createdFromLocationIds.addAll(data);
		}
		
		boolean hasAppliedPriviledgeFilter = false;
		if (createdFromLocationIds != null) {
			if (createdFromLocationIds.isEmpty())
				return new Result<>(new ArrayList<>(), 0);
			if (recordPrivilegeFilters != null) {
				hasAppliedPriviledgeFilter = true;
				final List<Integer> createdFromLocationIdsStream = createdFromLocationIds;
				createdFromLocationIds = recordPrivilegeFilters.stream()
				        .filter(p -> createdFromLocationIdsStream.contains(p.getLocationId())).map(p -> p.getLocationId())
				        .distinct().collect(Collectors.toList());
			}
			if (createdFromLocationIds.isEmpty())
				return new Result<>(new ArrayList<>(), 0);
			appendFilter(hqlFilter, "oi.createdFrom.locationId in (:createdFroms)");
			parameterWithList.put("createdFroms", createdFromLocationIds.stream().distinct().collect(Collectors.toList()));
		}
		
		List<Integer> fulfillmentLocationIds = null;
		if (filter.getFulfilmentLocationIds() != null && !filter.getFulfilmentLocationIds().isEmpty()) {
			fulfillmentLocationIds = fulfillmentLocationIds == null ? new ArrayList<>() : fulfillmentLocationIds;
			fulfillmentLocationIds.addAll(filter.getFulfilmentLocationIds());
		}
		if (filter.getFulfilmentLocationUuids() != null && !filter.getFulfilmentLocationUuids().isEmpty()) {
			PartySearchFilter partySearchFilter = new PartySearchFilter();
			partySearchFilter.setLocationUuids(filter.getFulfilmentLocationUuids());
			fulfillmentLocationIds = fulfillmentLocationIds == null ? new ArrayList<>() : fulfillmentLocationIds;
			List<Integer> data = findParty(partySearchFilter).getData().stream().map(p -> p.getLocationId())
			        .collect(Collectors.toList());
			if (data.isEmpty())
				return new Result<>(new ArrayList<>(), 0);
			fulfillmentLocationIds.addAll(data);
		}
		if (filter.getFulfilmentPartyUuids() != null && !filter.getFulfilmentPartyUuids().isEmpty()) {
			PartySearchFilter partySearchFilter = new PartySearchFilter();
			partySearchFilter.setPartyUuids(filter.getFulfilmentPartyUuids());
			fulfillmentLocationIds = fulfillmentLocationIds == null ? new ArrayList<>() : fulfillmentLocationIds;
			List<Integer> data = findParty(partySearchFilter).getData().stream().map(p -> p.getLocationId())
			        .collect(Collectors.toList());
			if (data.isEmpty())
				return new Result<>(new ArrayList<>(), 0);
			fulfillmentLocationIds.addAll(data);
		}
		
		if (fulfillmentLocationIds != null) {
			if (fulfillmentLocationIds.isEmpty())
				return new Result<>(new ArrayList<>(), 0);
			if (recordPrivilegeFilters != null) {
				hasAppliedPriviledgeFilter = true;
				final List<Integer> fulfillmentLocationIdsStream = fulfillmentLocationIds;
				fulfillmentLocationIds = recordPrivilegeFilters.stream()
				        .filter(p -> fulfillmentLocationIdsStream.contains(p.getLocationId())).map(p -> p.getLocationId())
				        .distinct().collect(Collectors.toList());
			}
			if (fulfillmentLocationIds.isEmpty())
				return new Result<>(new ArrayList<>(), 0);
			appendFilter(hqlFilter, "oi.fulfilmentLocation.locationId in (:fulfilmentLocations)");
			parameterWithList.put("fulfilmentLocations",
			    fulfillmentLocationIds.stream().distinct().collect(Collectors.toList()));
		}
		
		if (!hasAppliedPriviledgeFilter && recordPrivilegeFilters != null) {
			appendFilter(hqlFilter, "oi.fulfilmentLocation.locationId in (:rpfs) or oi.createdFrom.locationId in (:rpfs)");
			parameterWithList.put("rpfs",
			    recordPrivilegeFilters.stream().map(p -> p.getLocationId()).distinct().collect(Collectors.toList()));
		}
		
		if (filter.getOrderUuids() != null) {
			appendFilter(hqlFilter, "o.uuid in (:orderUuids)");
			parameterWithList.put("orderUuids", filter.getOrderUuids());
		}
		
		if (filter.getOrderDateMin() != null) {
			appendFilter(hqlFilter, "o.scheduledDate >= :odm");
			parameterList.putIfAbsent("odm", filter.getOrderDateMin());
		}
		
		if (filter.getOrderDateMax() != null) {
			appendFilter(hqlFilter, "o.scheduledDate <= :odmx");
			parameterList.putIfAbsent("odmx", filter.getOrderDateMax());
		}
		
		if ((filter.getStockItemIds() != null && !filter.getStockItemIds().isEmpty())
		        || (filter.getStockItemUuids() != null && !filter.getStockItemUuids().isEmpty())) {
			List<Integer> stockItemIds = new ArrayList<>();
			if (filter.getStockItemIds() != null) {
				stockItemIds.addAll(filter.getStockItemIds());
			}
			if (filter.getStockItemUuids() != null) {
				stockItemIds.addAll(getStockItemIds(filter.getStockItemUuids()).values());
			}
			if (stockItemIds.isEmpty())
				return new Result<>(new ArrayList<>(), 0);
			appendFilter(hqlFilter, "oi.stockItem.id in (:stockItemIds)");
			parameterWithList.put("stockItemIds", stockItemIds);
		}
		
		if (filter.getEncounterIds() != null && !filter.getEncounterIds().isEmpty()) {
			appendFilter(hqlFilter, "o.encounter.encounterId in (:encounterIds)");
			parameterWithList.put("encounterIds", filter.getEncounterIds());
		}
		
		if (filter.getEncounterUuids() != null && !filter.getEncounterUuids().isEmpty()) {
			appendFilter(hqlFilter, "e.uuid in (:encounterUuids)");
			parameterWithList.put("encounterUuids", filter.getEncounterUuids());
		}
		
		if (filter.getPatientIds() != null) {
			appendFilter(hqlFilter, "o.patient.patientId in (:patientIds)");
			parameterWithList.put("patientIds", filter.getPatientIds());
		}
		
		if (isNotNullOrEmpty(filter.getOrderNumber())) {
			String orderNumber = filter.getOrderNumber().replaceAll("%", "");
			if (orderNumber.length() == 0)
				return new Result<>(new ArrayList<>(), 0);
			orderNumber = orderNumber.toUpperCase() + "%";
			appendFilter(hqlFilter, "o.orderNumber like :orderNumber");
			parameterList.put("orderNumber", orderNumber);
		}
		
		if (filter.getIsDrug() != null) {
			if (filter.getIsDrug()) {
				appendFilter(hqlFilter, "si.drug.drugId is not null");
			} else {
				appendFilter(hqlFilter, "si.drug.drugId is null");
			}
		}
		
		if (filter.getSearchEitherDrugOrConceptStockItems()) {
			StringBuilder itemFilter = new StringBuilder();
			
			if (filter.getDrugIds() != null && !filter.getDrugIds().isEmpty()) {
				appendORFilter(itemFilter, "si.drug.drugId in (:drugIds)");
				parameterWithList.putIfAbsent("drugIds", filter.getDrugIds());
			}
			
			if (filter.getConceptIds() != null && !filter.getConceptIds().isEmpty()) {
				appendORFilter(itemFilter, "si.concept.conceptId in (:conceptIds)");
				parameterWithList.putIfAbsent("conceptIds", filter.getConceptIds());
			}
			
			if (filter.getDrugUuids() != null && !filter.getDrugUuids().isEmpty()) {
				appendORFilter(itemFilter, "d.uuid in (:drugUuids)");
				parameterWithList.putIfAbsent("drugUuids", filter.getDrugUuids());
			}
			
			if (filter.getConceptUuids() != null && !filter.getConceptUuids().isEmpty()) {
				appendORFilter(itemFilter, "c.uuid in (:conceptUuids)");
				parameterWithList.putIfAbsent("conceptUuids", filter.getConceptUuids());
			}
			
			if (itemFilter.length() > 0) {
				appendFilter(hqlFilter, itemFilter.toString());
			}
		} else {
			if (filter.getDrugIds() != null && !filter.getDrugIds().isEmpty()) {
				appendFilter(hqlFilter, "si.drug.drugId in (:drugIds)");
				parameterWithList.putIfAbsent("drugIds", filter.getDrugIds());
			}
			if (filter.getConceptIds() != null && !filter.getConceptIds().isEmpty()) {
				appendFilter(hqlFilter, "si.concept.conceptId in (:conceptIds)");
				parameterWithList.putIfAbsent("conceptIds", filter.getConceptIds());
			}
			
			if (filter.getDrugUuids() != null && !filter.getDrugUuids().isEmpty()) {
				appendFilter(hqlFilter, "d.uuid in (:drugUuids)");
				parameterWithList.putIfAbsent("drugUuids", filter.getDrugUuids());
			}
			
			if (filter.getConceptUuids() != null && !filter.getConceptUuids().isEmpty()) {
				appendFilter(hqlFilter, "c.uuid in (:conceptUuids)");
				parameterWithList.putIfAbsent("conceptUuids", filter.getConceptUuids());
			}
		}
		
		if (!filter.getIncludeVoided()) {
			appendFilter(hqlFilter, "oi.voided = :vdd");
			parameterList.putIfAbsent("vdd", false);
		}
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		Result<OrderItemDTO> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		
		result.setData(
		    executeQuery(OrderItemDTO.class, hqlQuery, result, " order by oi.id desc", parameterList, parameterWithList));
		
		if (!result.getData().isEmpty()) {
			List<Integer> conceptNamesToFetch = result.getData().stream()
			        .map(p -> Arrays.asList(p.getConceptId(), p.getStockItemPackagingUOMConceptId()))
			        .flatMap(Collection::stream).filter(p -> p != null).distinct().collect(Collectors.toList());
			Map<Integer, List<ConceptNameDTO>> conceptNameDTOs = conceptNamesToFetch.isEmpty() ? null
			        : getConceptNamesByConceptIds(conceptNamesToFetch).stream()
			                .collect(Collectors.groupingBy(ConceptNameDTO::getConceptId));
			
			PartySearchFilter partySearchFilter = new PartySearchFilter();
			partySearchFilter.setIncludeVoided(true);
			List<Integer> nameLocationIds = result.getData().stream()
			        .map(p -> Arrays.asList(p.getCreatedFrom(), p.getFulfilmentLocationId())).flatMap(Collection::stream)
			        .filter(p -> p != null).distinct().collect(Collectors.toList());
			nameLocationIds.addAll(nameLocationIds);
			partySearchFilter.setLocationIds(nameLocationIds);
			Map<Integer, List<PartyDTO>> partyNameMap = nameLocationIds.isEmpty() ? null
			        : findParty(partySearchFilter).getData().stream()
			                .collect(Collectors.groupingBy(PartyDTO::getLocationId));
			Map<Integer, Object[]> quantities = getOrderQuantities(
			    result.getData().stream().map(p -> p.getOrderId()).collect(Collectors.toList()));
			for (OrderItemDTO orderItemDTO : result.getData()) {
				
				if (conceptNameDTOs != null) {
					if (orderItemDTO.getConceptId() != null && conceptNameDTOs.containsKey(orderItemDTO.getConceptId())) {
						orderItemDTO.setConceptName(conceptNameDTOs.get(orderItemDTO.getConceptId()).get(0).getName());
					}
					
					if (orderItemDTO.getStockItemPackagingUOMConceptId() != null
					        && conceptNameDTOs.containsKey(orderItemDTO.getStockItemPackagingUOMConceptId())) {
						orderItemDTO.setStockItemPackagingUOMName(
						    conceptNameDTOs.get(orderItemDTO.getStockItemPackagingUOMConceptId()).get(0).getName());
					}
				}
				if (partyNameMap != null) {
					if (orderItemDTO.getCreatedFrom() != null) {
						List<PartyDTO> party = partyNameMap.getOrDefault(orderItemDTO.getCreatedFrom(), null);
						if (party != null) {
							orderItemDTO.setCreatedFromName(party.get(0).getName());
							orderItemDTO.setCreatedFromPartyUuid(party.get(0).getUuid());
						}
					}
					if (orderItemDTO.getFulfilmentLocationId() != null) {
						List<PartyDTO> party = partyNameMap.getOrDefault(orderItemDTO.getFulfilmentLocationId(), null);
						if (party != null) {
							orderItemDTO.setFulfilmentLocationName(party.get(0).getName());
							orderItemDTO.setFulfilmentPartyUuid(party.get(0).getUuid());
						}
					}
				}
				
				Object[] quantityInfo = quantities.getOrDefault(orderItemDTO.getOrderId(), null);
				if (quantityInfo != null) {
					if (quantityInfo[1] != null) {
						orderItemDTO.setQuantity(BigDecimal.valueOf(((Number) quantityInfo[1]).doubleValue()));
					}
					if (quantityInfo[2] != null) {
						orderItemDTO.setDuration(((Number) quantityInfo[2]).intValue());
					}
				}
			}
		}
		
		return result;
	}
	
	private Map<Integer, Object[]> getOrderQuantities(List<Integer> orderIds) {
		if (orderIds == null || orderIds.isEmpty()) {
			return new HashMap<>();
		}
		Criteria criteria = getSession().createCriteria(DrugOrder.class).add(Restrictions.in("orderId", orderIds));
		Projection projection1 = Projections.property("orderId");
		Projection projection2 = Projections.property("quantity");
		Projection projection3 = Projections.property("duration");
		ProjectionList pList = Projections.projectionList();
		pList.add(projection1);
		pList.add(projection2);
		pList.add(projection3);
		criteria.setProjection(pList);
		List result = criteria.list();
		HashMap<Integer, Object[]> ids = new HashMap<>();
		for (Object object : result) {
			Object[] row = (Object[]) object;
			ids.put((Integer) row[0], row);
		}
		return ids;
	}
	
	public OrderItem getOrderItemByUuid(String uuid) {
		return (OrderItem) getSession().createCriteria(OrderItem.class).add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	public OrderItem saveOrderItem(OrderItem orderItem) {
		getSession().saveOrUpdate(orderItem);
		return orderItem;
	}
	
	public void setStockItemInformation(List<StockItemInventory> reportStockItemInventories) {
		if (reportStockItemInventories == null || reportStockItemInventories.isEmpty())
			return;
		
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("select si.id as stockItemId,\n" + "si.drug.drugId as stockItemDrugId,\n"
		        + "si.concept.conceptId as stockItemConceptId,\n" + "si.commonName as commonName,\n"
		        + "si.acronym as acronym,\n" + "si.category.conceptId as stockItemCategoryConceptId,\n"
		        + "si.reorderLevel as reorderLevel,\n" + "rol.packagingUom.conceptId as reorderLevelUoMId,\n"
		        + "rol.factor as reorderLevelFactor\n"
		        + "from stockmanagement.StockItem si left join si.reorderLevelUOM rol");
		StringBuilder hqlFilter = new StringBuilder();
		appendFilter(hqlFilter, "si.id in (:ids)");
		parameterWithList.putIfAbsent("ids",
		    reportStockItemInventories.stream().map(p -> p.getStockItemId()).distinct().collect(Collectors.toList()));
		hqlQuery.append(" where ");
		hqlQuery.append(hqlFilter);
		
		Result<StockBatchLineItem> result = new Result<>();
		result.setData(executeQuery(StockBatchLineItem.class, hqlQuery, result, null, new HashMap<>(), parameterWithList));
		
		if (!result.getData().isEmpty()) {
			List<Integer> conceptIds = new ArrayList<>();
			List<Integer> drugIds = new ArrayList<>();
			
			conceptIds.addAll(result.getData().stream().filter(p -> p.getStockItemCategoryConceptId() != null)
			        .map(p -> p.getStockItemCategoryConceptId()).collect(Collectors.toList()));
			conceptIds.addAll(result.getData().stream().filter(p -> p.getStockItemConceptId() != null)
			        .map(p -> p.getStockItemConceptId()).collect(Collectors.toList()));
			conceptIds.addAll(result.getData().stream().filter(p -> p.getReorderLevelUoMId() != null)
			        .map(p -> p.getReorderLevelUoMId()).collect(Collectors.toList()));
			drugIds.addAll(result.getData().stream().filter(p -> p.getStockItemDrugId() != null)
			        .map(p -> p.getStockItemDrugId()).collect(Collectors.toList()));
			
			Map<Integer, List<ConceptNameDTO>> conceptNameDTOs = null;
			if (conceptIds.isEmpty()) {
				conceptNameDTOs = new HashMap<>();
			} else {
				conceptNameDTOs = getConceptNamesByConceptIds(conceptIds.stream().distinct().collect(Collectors.toList()))
				        .stream().collect(Collectors.groupingBy(p -> p.getConceptId()));
			}
			
			Map<Integer, List<ConceptNameDTO>> drugNames = null;
			if (drugIds.isEmpty()) {
				drugNames = new HashMap<>();
			} else {
				drugNames = getDrugNamesByDrugIds(drugIds).stream().collect(Collectors.groupingBy(p -> p.getConceptId()));
			}
			
			Map<Integer, List<StockBatchLineItem>> dataGroup = result.getData().stream()
			        .collect(Collectors.groupingBy(p -> p.getStockItemId()));
			for (StockItemInventory reportStockItemInventory : reportStockItemInventories) {
				
				List<StockBatchLineItem> infoLineItem = dataGroup.get(reportStockItemInventory.getStockItemId());
				if (infoLineItem == null) {
					continue;
				}
				reportStockItemInventory.setConceptId(infoLineItem.get(0).getStockItemConceptId());
				reportStockItemInventory.setDrugId(infoLineItem.get(0).getStockItemDrugId());
				reportStockItemInventory.setCommonName(infoLineItem.get(0).getCommonName());
				reportStockItemInventory.setAcronym(infoLineItem.get(0).getAcronym());
				
				reportStockItemInventory.setReorderLevel(infoLineItem.get(0).getReorderLevel());
				reportStockItemInventory.setReorderLevelFactor(infoLineItem.get(0).getReorderLevelFactor());
				
				List<ConceptNameDTO> conceptNameDTO = null;
				
				if (infoLineItem.get(0).getStockItemCategoryConceptId() != null) {
					conceptNameDTO = conceptNameDTOs.get(infoLineItem.get(0).getStockItemCategoryConceptId());
					if (conceptNameDTO != null) {
						reportStockItemInventory.setStockItemCategoryName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (reportStockItemInventory.getConceptId() != null) {
					conceptNameDTO = conceptNameDTOs.get(reportStockItemInventory.getConceptId());
					if (conceptNameDTO != null) {
						reportStockItemInventory.setConceptName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (reportStockItemInventory.getDrugId() != null) {
					conceptNameDTO = drugNames.get(reportStockItemInventory.getDrugId());
					if (conceptNameDTO != null) {
						reportStockItemInventory.setDrugName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (infoLineItem.get(0).getReorderLevelUoMId() != null) {
					conceptNameDTO = conceptNameDTOs.get(infoLineItem.get(0).getReorderLevelUoMId());
					if (conceptNameDTO != null) {
						reportStockItemInventory.setReorderLevelUoM(conceptNameDTO.get(0).getName());
					}
				}
			}
		}
	}
	
	public Result<DispensingLineItem> findDispensingLineItems(DispensingLineFilter filter) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder(
		        "SELECT sit.id as stockItemTransactionId, sit.dateCreated as dateCreated,\n"
		                + "sit.creator.userId as creator,\n" + "sit.party.id as partyId,\n" + "pl.name as partyName,\n"
		                + "sit.stockItem.id as stockItemId,\n" + "si.drug.drugId as stockItemDrugId,\n"
		                + "si.concept.conceptId as stockItemConceptId,\n" + "si.commonName as commonName,\n"
		                + "si.acronym as acronym,\n" + "si.category.conceptId as stockItemCategoryConceptId,\n"
		                + "sipu.packagingUom.conceptId as packagingUoMId,\n"
		                + "sipu.factor as stockItemPackagingUOMFactor,\n" + "sb.batchNo as batchNo,\n"
		                + "sb.expiration as expiration,\n" + "sit.quantity as quantity,"
		                + "sit.patient.id as patientId, sit.order.orderId as orderId,\n" + "od.orderNumber as orderNumber\n"
		                + "from stockmanagement.StockItemTransaction sit join\n"
		                + "\t sit.stockItem si left join sit.party p left join p.location pl left join\n"
		                + " sit.stockBatch sb left join\n" + " sit.stockItemPackagingUOM sipu left join\n"
		                + " sit.order od");
		StringBuilder hqlFilter = new StringBuilder();
		
		if (filter.getStockItemTransactionMin() != null) {
			appendFilter(hqlFilter, "sit.id > :stockItemTransactionId");
			parameterList.putIfAbsent("stockItemTransactionId", filter.getStockItemTransactionMin());
		}
		
		if (filter.getAtLocationId() != null) {
			boolean filterSet = false;
			Location location = Context.getLocationService().getLocation(filter.getAtLocationId());
			if (location == null) {
				return new Result<>(new ArrayList<>(), 0);
			}
			Party party = getPartyByLocation(location);
			if (party == null) {
				return new Result<>(new ArrayList<>(), 0);
			}
			if (filter.getChildLocations() != null && filter.getChildLocations()) {
				if (party.getLocation() != null) {
					List<Integer> locationIds = getCompleteLocationTree(party.getLocation().getLocationId()).stream()
					        .map(p -> p.getChildLocationId()).collect(Collectors.toList());
					if (locationIds.isEmpty()) {
						locationIds.add(filter.getAtLocationId());
					}
					Map<Integer, Integer> partLocationIds = getLocationPartyIds(locationIds);
					if (!partLocationIds.isEmpty()) {
						appendFilter(hqlFilter, "sit.party.id in (:partyIds)");
						parameterWithList.putIfAbsent("partyIds", partLocationIds.values());
						filterSet = true;
					}
				}
			}
			if (!filterSet) {
				appendFilter(hqlFilter, "sit.party.id = :partyId");
				parameterList.putIfAbsent("partyId", party.getId());
			}
		}
		
		if (filter.getStockItemId() != null) {
			appendFilter(hqlFilter, "sit.stockItem.id = :stockItemId");
			parameterList.putIfAbsent("stockItemId", filter.getStockItemId());
		}
		
		if (filter.getPatientId() != null) {
			appendFilter(hqlFilter, "sit.patient.patientId = :patientId");
			parameterList.putIfAbsent("patientId", filter.getPatientId());
		} else {
			appendFilter(hqlFilter, "sit.patient.patientId is not null");
		}
		
		if (filter.getStartDate() != null) {
			appendFilter(hqlFilter, "sit.dateCreated >= :sitdcm");
			parameterList.putIfAbsent("sitdcm", filter.getStartDate());
		}
		
		if (filter.getEndDate() != null) {
			appendFilter(hqlFilter, "sit.dateCreated <= :sitdcmx");
			parameterList.putIfAbsent("sitdcmx", filter.getEndDate());
		}
		
		if (filter.getStockItemCategoryConceptId() != null) {
			appendFilter(hqlFilter, "si.category.conceptId = :stockItemCategoryId");
			parameterList.putIfAbsent("stockItemCategoryId", filter.getStockItemCategoryConceptId());
		}
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		Result<DispensingLineItem> result = new Result<>();
		org.hibernate.StatelessSession dbSession = null;
		try {
			dbSession = getStatelessHibernateSession();
			hqlQuery.append(" order by sit.id asc");
			Query query = dbSession.createQuery(hqlQuery.toString());
			query.setReadOnly(true);
			if (parameterList != null) {
				for (Map.Entry<String, Object> entry : parameterList.entrySet())
					query.setParameter(entry.getKey(), entry.getValue());
			}
			if (parameterWithList != null) {
				for (Map.Entry<String, Collection> entry : parameterWithList.entrySet())
					query.setParameterList(entry.getKey(), entry.getValue());
			}
			
			query = query.setResultTransformer(new AliasToBeanResultTransformer(DispensingLineItem.class));
			if (filter.getStockItemTransactionMin() != null) {
				query.setFirstResult(0);
			} else {
				query.setFirstResult(filter.getLimit() * filter.getStartIndex());
			}
			query.setMaxResults(filter.getLimit());
			query.setFetchSize(filter.getLimit());
			result.setData(query.list());
		}
		finally {
			if (dbSession != null) {
				try {
					dbSession.close();
				}
				catch (Exception e) {}
			}
		}
		
		if (!result.getData().isEmpty()) {
			List<Integer> conceptIds = new ArrayList<>();
			List<Integer> drugIds = new ArrayList<>();
			
			conceptIds.addAll(result.getData().stream().filter(p -> p.getPackagingUoMId() != null)
			        .map(p -> p.getPackagingUoMId()).collect(Collectors.toList()));
			conceptIds.addAll(result.getData().stream().filter(p -> p.getStockItemCategoryConceptId() != null)
			        .map(p -> p.getStockItemCategoryConceptId()).collect(Collectors.toList()));
			conceptIds.addAll(result.getData().stream().filter(p -> p.getStockItemConceptId() != null)
			        .map(p -> p.getStockItemConceptId()).collect(Collectors.toList()));
			drugIds.addAll(result.getData().stream().filter(p -> p.getStockItemDrugId() != null)
			        .map(p -> p.getStockItemDrugId()).collect(Collectors.toList()));
			List<Integer> userIds = result.getData().stream().map(p -> p.getCreator()).filter(p -> p != null)
			        .collect(Collectors.toList());
			List<Integer> patientIds = result.getData().stream().map(p -> p.getPatientId()).filter(p -> p != null)
			        .collect(Collectors.toList());
			
			Map<Integer, List<ConceptNameDTO>> conceptNameDTOs = null;
			if (conceptIds.isEmpty()) {
				conceptNameDTOs = new HashMap<>();
			} else {
				conceptNameDTOs = getConceptNamesByConceptIds(conceptIds.stream().distinct().collect(Collectors.toList()))
				        .stream().collect(Collectors.groupingBy(p -> p.getConceptId()));
			}
			
			Map<Integer, List<ConceptNameDTO>> drugNames = null;
			if (drugIds.isEmpty()) {
				drugNames = new HashMap<>();
			} else {
				drugNames = getDrugNamesByDrugIds(drugIds).stream().collect(Collectors.groupingBy(p -> p.getConceptId()));
			}
			
			List<Integer> distinctUserIds = userIds.stream().distinct().collect(Collectors.toList());
			Map<Integer, List<UserPersonNameDTO>> personNames = getPersonNameByUserIds(distinctUserIds).stream()
			        .collect(Collectors.groupingBy(p -> p.getUserId()));
			List<Integer> distinctPatientIds = patientIds.stream().distinct().collect(Collectors.toList());
			Map<Integer, List<UserPersonNameDTO>> patientNames = getPatientNameByPatientIds(distinctPatientIds, true)
			        .stream().collect(Collectors.groupingBy(p -> p.getPatientId()));
			
			for (DispensingLineItem dispensingLineItem : result.getData()) {
				
				List<ConceptNameDTO> conceptNameDTO = null;
				
				if (dispensingLineItem.getPackagingUoMId() != null) {
					conceptNameDTO = conceptNameDTOs.get(dispensingLineItem.getPackagingUoMId());
					if (conceptNameDTO != null) {
						dispensingLineItem.setStockItemPackagingUOMName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (dispensingLineItem.getStockItemCategoryConceptId() != null) {
					conceptNameDTO = conceptNameDTOs.get(dispensingLineItem.getStockItemCategoryConceptId());
					if (conceptNameDTO != null) {
						dispensingLineItem.setStockItemCategoryName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (dispensingLineItem.getStockItemConceptId() != null) {
					conceptNameDTO = conceptNameDTOs.get(dispensingLineItem.getStockItemConceptId());
					if (conceptNameDTO != null) {
						dispensingLineItem.setStockItemConceptName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (dispensingLineItem.getStockItemDrugId() != null) {
					conceptNameDTO = drugNames.get(dispensingLineItem.getStockItemDrugId());
					if (conceptNameDTO != null) {
						dispensingLineItem.setStockItemDrugName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (dispensingLineItem.getCreator() != null) {
					List<UserPersonNameDTO> userPersonNameDTO = personNames.get(dispensingLineItem.getCreator());
					if (userPersonNameDTO != null) {
						dispensingLineItem.setCreatorFamilyName(userPersonNameDTO.get(0).getFamilyName());
						dispensingLineItem.setCreatorGivenName(userPersonNameDTO.get(0).getGivenName());
					}
				}
				
				if (dispensingLineItem.getPatientId() != null) {
					List<UserPersonNameDTO> userPersonNameDTO = patientNames.get(dispensingLineItem.getPatientId());
					if (userPersonNameDTO != null) {
						dispensingLineItem.setPatientFamilyName(userPersonNameDTO.get(0).getFamilyName());
						dispensingLineItem.setPatientMiddleName(userPersonNameDTO.get(0).getMiddleName());
						dispensingLineItem.setPatientGivenName(userPersonNameDTO.get(0).getGivenName());
						dispensingLineItem.setPatientIdentifier(userPersonNameDTO.get(0).getPatientIdentifier());
					}
				}
				
			}
			
		}
		
		return result;
	}
	
	public Result<PrescriptionLineItem> findPrescriptionLineItems(PrescriptionLineFilter filter) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		
		boolean addFullfillmentInfo = filter.getFullfillments() != null && !filter.getFullfillments().isEmpty();
		
		StringBuilder hqlQuery = new StringBuilder("SELECT o.order_id as id, o.date_created as dateCreated,\n"
		        + "o.order_id as orderId,\n" + "o.previous_order_id as previousOrderId,\n"
		        + "o.date_activated as dateActivated,\n" + "o.date_stopped as dateStopped,\n"
		        + "o.patient_id as patientId,\n" + "op.person_id as ordererPersonId,\n" + "o.order_action as action,\n"
		        + "o.urgency as urgency,\n" + "si.stock_item_id as stockItemId,\n" + "si.drug_id as stockItemDrugId,\n"
		        + "si.concept_id as stockItemConceptId,\n" + "si.common_name as commonName,\n" + "si.acronym as acronym,\n"
		        + "si.category_id as stockItemCategoryConceptId,\n" + "do.quantity_units as packagingUoMId,\n"
		        + "COALESCE(sipu.factor, sipu2.factor, 1) as stockItemPackagingUOMFactor,\n" + "do.quantity as quantity,"
		        + "cf.name as createdFrom,\n"
		        + "(select value_text from obs obs  where obs.encounter_id = o.encounter_id and obs.concept_id = :dispensingLocationConceptId and obs_group_id = (\n"
		        + "select obsdg.obs_group_id \n" + "from obs obsdg \n"
		        + "where obsdg.encounter_id = o.encounter_id and obsdg.concept_id = :drugConceptId and obsdg.value_text=d.uuid \n"
		        + "order by obsdg.order_id desc limit 1\n"
		        + ") order by obs.order_id desc  limit 1) as fulfilmentLocationUuid,\n" + "do.dose as dose,"
		        + "do.dose_units as doseUnitsConceptId," + "of.frequency_per_day as frequencyPerDay,"
		        + "of.concept_id as frequencyConceptId," + "do.as_needed as asNeeded,"
		        + "do.quantity_units as quantityUnitsConceptId," + "do.as_needed_condition as asNeededCondition,"
		        + "do.num_refills as numRefills," + "do.dosing_instructions as dosingInstructions,"
		        + "do.duration as duration," + "do.duration_units as durationUnitsConceptId," + "do.route as routeConceptId,"
		        + "do.dispense_as_written as dispenseAsWritten,"
		        + (addFullfillmentInfo
		                ? ("sit.quantity as quantityDispensed, sitsipu.packaging_uom_id quantityDispensedPackagingUoMId,"
		                        + "sitsipu.factor as quantityDispensedStockItemPackagingUOMFactor,"
		                        + "sit.stock_item_transaction_id as stockItemTransactionId,"
		                        + "sit.creator as dispenserUserId,spl.name as dispensingLocation,"
		                        + "sit.date_created as dateDispensed, sb.batch_no as batchNo, sb.expiration as batchExpiryDate,")
		                : "")
		        + "o.order_number as orderNumber" + " from orders o join drug_order do on o.order_id = do.order_id left join"
		        + " drug d on do.drug_inventory_id = d.drug_id left join"
		        + " order_frequency of on do.frequency = of.order_frequency_id left join"
		        + " provider op on o.orderer = op.provider_id left join"
		        + " encounter e on o.encounter_id = e.encounter_id left join"
		        + " location cf on e.location_id = cf.location_id left join"
		        + " stockmgmt_stock_item si on do.drug_inventory_id = si.drug_id left join"
		        + " stockmgmt_stock_item_packaging_uom sipu on si.stock_item_id = sipu.stock_item_id and do.quantity_units=sipu.packaging_uom_id left join"
		        + " stockmgmt_stock_item_packaging_uom sipu2 on si.stock_item_id = sipu.stock_item_id and do.quantity_units=si.dispensing_unit_id and si.dispensing_unit_packaging_uom_id=sipu.stock_item_packaging_uom_id ");
		if (addFullfillmentInfo) {
			hqlQuery.append(" left join stockmgmt_stock_item_transaction sit on o.order_id=sit.order_id left join"
			        + " stockmgmt_party sp on sit.party_id=sp.party_id left join location spl on sp.location_id = spl.location_id left join"
			        + " stockmgmt_stock_batch sb on sit.stock_batch_id = sb.stock_batch_id left join"
			        + " stockmgmt_stock_item_packaging_uom sitsipu on sit.stock_item_packaging_uom_id = sitsipu.stock_item_packaging_uom_id");
		}
		
		StringBuilder hqlFilter = new StringBuilder();
		if (filter.getPrescriptionTransactionMin() != null) {
			appendFilter(hqlFilter, "o.order_id > :transactionIdMin");
			parameterList.putIfAbsent("transactionIdMin", filter.getPrescriptionTransactionMin());
		}
		
		if (filter.getAtLocationId() != null) {
			boolean filterSet = false;
			Location location = Context.getLocationService().getLocation(filter.getAtLocationId());
			if (location == null) {
				return new Result<>(new ArrayList<>(), 0);
			}
			if (filter.getChildLocations() != null && filter.getChildLocations()) {
				List<Integer> locationIds = getCompleteLocationTree(filter.getAtLocationId()).stream()
				        .map(p -> p.getChildLocationId()).collect(Collectors.toList());
				if (locationIds.isEmpty()) {
					locationIds.add(filter.getAtLocationId());
				}
				
				appendFilter(hqlFilter, "e.location_id in (:locationIds)");
				parameterWithList.putIfAbsent("locationIds", locationIds);
				filterSet = true;
			}
			if (!filterSet) {
				appendFilter(hqlFilter, "e.location_id = :locationId");
				parameterList.putIfAbsent("locationId", filter.getAtLocationId());
			}
		}
		
		if (filter.getDrugId() != null) {
			appendFilter(hqlFilter, "do.drug_inventory_id = :drugId");
			parameterList.putIfAbsent("drugId", filter.getDrugId());
		}
		
		if (filter.getStockItemId() != null) {
			appendFilter(hqlFilter, "si.stock_item_id = :stockItemId");
			parameterList.putIfAbsent("stockItemId", filter.getStockItemId());
		}
		
		if (filter.getPatientId() != null) {
			appendFilter(hqlFilter, "o.patient_id = :patientId");
			parameterList.putIfAbsent("patientId", filter.getPatientId());
		}
		
		if (filter.getStartDate() != null) {
			appendFilter(hqlFilter, "o.date_created >= :sitdcm");
			parameterList.putIfAbsent("sitdcm", filter.getStartDate());
		}
		
		if (filter.getEndDate() != null) {
			appendFilter(hqlFilter, "o.date_created <= :sitdcmx");
			parameterList.putIfAbsent("sitdcmx", filter.getEndDate());
		}
		
		if (filter.getStockItemCategoryConceptId() != null) {
			appendFilter(hqlFilter, "si.category_id = :stockItemCategoryId");
			parameterList.putIfAbsent("stockItemCategoryId", filter.getStockItemCategoryConceptId());
		}
		
		appendFilter(hqlFilter, "do.quantity > 0");
		
		if (addFullfillmentInfo) {
			boolean hasPartial = false, hasFull = false, hasNone = false, hasAll = false;
			for (Fullfillment fullfillment : filter.getFullfillments()) {
				switch (fullfillment) {
					case All:
						hasAll = true;
						break;
					case Full:
						hasFull = true;
						break;
					case Partial:
						hasPartial = true;
						break;
					case None:
						hasNone = true;
						break;
				}
			}
			if (!(hasAll || (hasPartial && hasNone && hasFull))) {
				StringBuilder fullFillmentFilter = new StringBuilder();
				if (hasFull && hasPartial) {
					appendFilter(fullFillmentFilter, "sit.stock_item_transaction_id is not null");
				} else if (hasFull) {
					appendFilter(fullFillmentFilter,
					    "(sit.quantity * sitsipu.factor * -1) >= (do.quantity * COALESCE(sipu.factor, sipu2.factor, 1))");
				} else if (hasPartial) {
					appendFilter(fullFillmentFilter,
					    "(sit.quantity * sitsipu.factor * -1) < (do.quantity * COALESCE(sipu.factor, sipu2.factor, 1))");
				}
				if (hasNone) {
					appendORFilter(fullFillmentFilter, "sit.stock_item_transaction_id is null");
				}
				appendFilter(hqlFilter, fullFillmentFilter.toString());
			}
		}
		
		parameterList.put("dispensingLocationConceptId", filter.getObservationDispensingLocationConceptId());
		parameterList.put("drugConceptId", filter.getObservationDrugConceptId());
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		Result<PrescriptionLineItem> result = new Result<>();
		org.hibernate.StatelessSession dbSession = null;
		try {
			dbSession = getStatelessHibernateSession();
			hqlQuery.append(" order by o.order_id asc");
			Query query = dbSession.createSQLQuery(hqlQuery.toString());
			query.setReadOnly(true);
			if (parameterList != null) {
				for (Map.Entry<String, Object> entry : parameterList.entrySet())
					query.setParameter(entry.getKey(), entry.getValue());
			}
			if (parameterWithList != null) {
				for (Map.Entry<String, Collection> entry : parameterWithList.entrySet())
					query.setParameterList(entry.getKey(), entry.getValue());
			}
			
			query = query.setResultTransformer(new AliasToBeanResultTransformer(PrescriptionLineItem.class));
			if (filter.getPrescriptionTransactionMin() != null) {
				query.setFirstResult(0);
			} else {
				query.setFirstResult(filter.getLimit() * filter.getStartIndex());
			}
			query.setMaxResults(filter.getLimit());
			query.setFetchSize(filter.getLimit());
			result.setData(query.list());
		}
		finally {
			if (dbSession != null) {
				try {
					dbSession.close();
				}
				catch (Exception e) {}
			}
		}
		
		if (!result.getData().isEmpty()) {
			List<Integer> conceptIds = new ArrayList<>();
			List<Integer> drugIds = new ArrayList<>();
			List<String> fullfillmentLocationUuids = new ArrayList<>();
			
			conceptIds.addAll(result.getData().stream().filter(p -> p.getPackagingUoMId() != null)
			        .map(p -> p.getPackagingUoMId()).collect(Collectors.toList()));
			conceptIds.addAll(result.getData().stream().filter(p -> p.getStockItemCategoryConceptId() != null)
			        .map(p -> p.getStockItemCategoryConceptId()).collect(Collectors.toList()));
			conceptIds.addAll(result.getData().stream().filter(p -> p.getStockItemConceptId() != null)
			        .map(p -> p.getStockItemConceptId()).collect(Collectors.toList()));
			
			conceptIds.addAll(result.getData().stream().filter(p -> p.getDoseUnitsConceptId() != null)
			        .map(p -> p.getDoseUnitsConceptId()).collect(Collectors.toList()));
			conceptIds.addAll(result.getData().stream().filter(p -> p.getFrequencyConceptId() != null)
			        .map(p -> p.getFrequencyConceptId()).collect(Collectors.toList()));
			conceptIds.addAll(result.getData().stream().filter(p -> p.getQuantityUnitsConceptId() != null)
			        .map(p -> p.getQuantityUnitsConceptId()).collect(Collectors.toList()));
			conceptIds.addAll(result.getData().stream().filter(p -> p.getDurationUnitsConceptId() != null)
			        .map(p -> p.getDurationUnitsConceptId()).collect(Collectors.toList()));
			conceptIds.addAll(result.getData().stream().filter(p -> p.getRouteConceptId() != null)
			        .map(p -> p.getRouteConceptId()).collect(Collectors.toList()));
			
			if (addFullfillmentInfo) {
				conceptIds.addAll(result.getData().stream().filter(p -> p.getQuantityDispensedPackagingUoMId() != null)
				        .map(p -> p.getQuantityDispensedPackagingUoMId()).collect(Collectors.toList()));
			}
			
			drugIds.addAll(result.getData().stream().filter(p -> p.getStockItemDrugId() != null)
			        .map(p -> p.getStockItemDrugId()).collect(Collectors.toList()));
			List<Integer> userIds = result.getData().stream().map(p -> p.getOrdererPersonId()).filter(p -> p != null)
			        .collect(Collectors.toList());
			if (addFullfillmentInfo) {
				userIds.addAll(result.getData().stream().map(p -> p.getDispenserUserId()).filter(p -> p != null)
				        .collect(Collectors.toList()));
			}
			List<Integer> patientIds = result.getData().stream().map(p -> p.getPatientId()).filter(p -> p != null)
			        .collect(Collectors.toList());
			fullfillmentLocationUuids = result.getData().stream().map(p -> p.getFulfilmentLocationUuid())
			        .filter(p -> p != null).distinct().collect(Collectors.toList());
			
			//getPatientNameByPatientIds
			Map<Integer, List<ConceptNameDTO>> conceptNameDTOs = null;
			if (conceptIds.isEmpty()) {
				conceptNameDTOs = new HashMap<>();
			} else {
				conceptNameDTOs = getConceptNamesByConceptIds(conceptIds.stream().distinct().collect(Collectors.toList()))
				        .stream().collect(Collectors.groupingBy(p -> p.getConceptId()));
			}
			
			Map<Integer, List<ConceptNameDTO>> drugNames = null;
			if (drugIds.isEmpty()) {
				drugNames = new HashMap<>();
			} else {
				drugNames = getDrugNamesByDrugIds(drugIds).stream().collect(Collectors.groupingBy(p -> p.getConceptId()));
			}
			
			List<Integer> distinctUserIds = userIds.stream().distinct().collect(Collectors.toList());
			Map<Integer, List<UserPersonNameDTO>> personNames = getPersonNameByUserIds(distinctUserIds).stream()
			        .collect(Collectors.groupingBy(p -> p.getUserId()));
			List<Integer> distinctPatientIds = patientIds.stream().distinct().collect(Collectors.toList());
			Map<Integer, List<UserPersonNameDTO>> patientNames = getPatientNameByPatientIds(distinctPatientIds, true)
			        .stream().collect(Collectors.groupingBy(p -> p.getPatientId()));
			Map<String, String> locationNames = getLocationNamesByUuid(fullfillmentLocationUuids);
			
			for (PrescriptionLineItem prescriptionLineItem : result.getData()) {
				
				List<ConceptNameDTO> conceptNameDTO = null;
				
				if (prescriptionLineItem.getPackagingUoMId() != null) {
					conceptNameDTO = conceptNameDTOs.get(prescriptionLineItem.getPackagingUoMId());
					if (conceptNameDTO != null) {
						prescriptionLineItem.setStockItemPackagingUOMName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (prescriptionLineItem.getStockItemCategoryConceptId() != null) {
					conceptNameDTO = conceptNameDTOs.get(prescriptionLineItem.getStockItemCategoryConceptId());
					if (conceptNameDTO != null) {
						prescriptionLineItem.setStockItemCategoryName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (prescriptionLineItem.getStockItemConceptId() != null) {
					conceptNameDTO = conceptNameDTOs.get(prescriptionLineItem.getStockItemConceptId());
					if (conceptNameDTO != null) {
						prescriptionLineItem.setStockItemConceptName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (prescriptionLineItem.getDoseUnitsConceptId() != null) {
					conceptNameDTO = conceptNameDTOs.get(prescriptionLineItem.getDoseUnitsConceptId());
					if (conceptNameDTO != null) {
						prescriptionLineItem.setDoseUnitsConceptName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (prescriptionLineItem.getFrequencyConceptId() != null) {
					conceptNameDTO = conceptNameDTOs.get(prescriptionLineItem.getFrequencyConceptId());
					if (conceptNameDTO != null) {
						prescriptionLineItem.setFrequencyConceptName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (prescriptionLineItem.getQuantityUnitsConceptId() != null) {
					conceptNameDTO = conceptNameDTOs.get(prescriptionLineItem.getQuantityUnitsConceptId());
					if (conceptNameDTO != null) {
						prescriptionLineItem.setQuantityUnitsConceptName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (prescriptionLineItem.getDurationUnitsConceptId() != null) {
					conceptNameDTO = conceptNameDTOs.get(prescriptionLineItem.getDurationUnitsConceptId());
					if (conceptNameDTO != null) {
						prescriptionLineItem.setDurationUnitsConceptName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (prescriptionLineItem.getRouteConceptId() != null) {
					conceptNameDTO = conceptNameDTOs.get(prescriptionLineItem.getRouteConceptId());
					if (conceptNameDTO != null) {
						prescriptionLineItem.setRouteConceptName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (prescriptionLineItem.getQuantityDispensedPackagingUoMId() != null) {
					conceptNameDTO = conceptNameDTOs.get(prescriptionLineItem.getQuantityDispensedPackagingUoMId());
					if (conceptNameDTO != null) {
						prescriptionLineItem.setQuantityDispensedStockItemPackagingUOMName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (prescriptionLineItem.getStockItemDrugId() != null) {
					conceptNameDTO = drugNames.get(prescriptionLineItem.getStockItemDrugId());
					if (conceptNameDTO != null) {
						prescriptionLineItem.setStockItemDrugName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (prescriptionLineItem.getOrdererPersonId() != null) {
					List<UserPersonNameDTO> userPersonNameDTO = personNames.get(prescriptionLineItem.getOrdererPersonId());
					if (userPersonNameDTO != null) {
						prescriptionLineItem.setOrdererFamilyName(userPersonNameDTO.get(0).getFamilyName());
						prescriptionLineItem.setOrdererMiddleName(userPersonNameDTO.get(0).getMiddleName());
						prescriptionLineItem.setOrdererGivenName(userPersonNameDTO.get(0).getGivenName());
					}
				}
				
				if (prescriptionLineItem.getDispenserUserId() != null) {
					List<UserPersonNameDTO> userPersonNameDTO = personNames.get(prescriptionLineItem.getDispenserUserId());
					if (userPersonNameDTO != null) {
						prescriptionLineItem.setDispenserFamilyName(userPersonNameDTO.get(0).getFamilyName());
						prescriptionLineItem.setDispenserMiddleName(userPersonNameDTO.get(0).getMiddleName());
						prescriptionLineItem.setDispenserGivenName(userPersonNameDTO.get(0).getGivenName());
					}
				}
				
				if (prescriptionLineItem.getPatientId() != null) {
					List<UserPersonNameDTO> userPersonNameDTO = patientNames.get(prescriptionLineItem.getPatientId());
					if (userPersonNameDTO != null) {
						prescriptionLineItem.setPatientFamilyName(userPersonNameDTO.get(0).getFamilyName());
						prescriptionLineItem.setPatientMiddleName(userPersonNameDTO.get(0).getMiddleName());
						prescriptionLineItem.setPatientGivenName(userPersonNameDTO.get(0).getGivenName());
						prescriptionLineItem.setPatientIdentifier(userPersonNameDTO.get(0).getPatientIdentifier());
					}
				}
				
				if (!locationNames.isEmpty() && !StringUtils.isBlank(prescriptionLineItem.getFulfilmentLocationUuid())) {
					String locationName = locationNames.get(prescriptionLineItem.getFulfilmentLocationUuid());
					if (locationName != null) {
						prescriptionLineItem.setFulfilmentLocation(locationName);
					}
				}
				
			}
			
		}
		
		return result;
	}
	
	public Map<Integer, Boolean> checkStockBatchHasTransactionsAfterOperation(Integer stockOperationId,
	        List<Integer> stockBatchIds) {
		if (stockOperationId == null || stockBatchIds == null || stockBatchIds.isEmpty()) {
			return new HashMap<>();
		}
		
		DbSession session = getSession();
		Query query = session.createQuery(
		    "SELECT max(sit.id) as id from stockmanagement.StockItemTransaction sit where sit.stockOperation.id = :soid");
		query.setParameter("soid", stockOperationId);
		List result = query.list();
		if (result.isEmpty() || (result.get(0) == null)) {
			return new HashMap<>();
		}
		int maxStockOperationItemTransactionId = ((Number) result.get(0)).intValue();
		query = session.createQuery(
		    "SELECT sit.stockBatch.id as id from stockmanagement.StockItemTransaction sit where sit.id > :id and sit.stockBatch.id in (:batchids) group by sit.stockBatch.id");
		query.setParameter("id", maxStockOperationItemTransactionId);
		query.setParameterList("batchids", stockBatchIds);
		result = query.list();
		if (result.isEmpty()) {
			return new HashMap<>();
		}
		
		Map<Integer, Boolean> mapResult = new HashMap<>();
		for (Object object : result) {
			mapResult.put(((Number) object).intValue(), Boolean.TRUE);
		}
		
		return mapResult;
	}
	
}
