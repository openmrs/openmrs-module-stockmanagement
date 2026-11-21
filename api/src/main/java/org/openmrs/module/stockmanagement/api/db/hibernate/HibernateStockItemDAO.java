/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.db.hibernate;

import org.openmrs.module.stockmanagement.api.dao.DaoBase;
import org.openmrs.module.stockmanagement.api.db.StockItemDAO;
import org.openmrs.module.stockmanagement.api.search.StockItemSearch;
import org.apache.commons.lang.StringUtils;
import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.criterion.Order;
import org.openmrs.*;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.module.stockmanagement.api.dto.*;
import org.openmrs.module.stockmanagement.api.model.*;
import org.openmrs.module.stockmanagement.api.utils.DateUtil;

import java.util.*;
import java.util.stream.Collectors;

public class HibernateStockItemDAO extends DaoBase implements StockItemDAO {
	
	private static final int STOCK_ITEM_FETCH_BATCH_SIZE = 100;
	
	@SuppressWarnings("unchecked")
	private <T> List<T> executeEntityQuery(StringBuilder hql, Result<T> paging, String order,
	        HashMap<String, Object> parameters, HashMap<String, Collection> lists) {
		loadPagingTotal(paging, hql, parameters, lists);
		Query query = getSession().createQuery(hql.toString() + (order == null ? "" : order));
		if (parameters != null)
			parameters.forEach(query::setParameter);
		if (lists != null)
			lists.forEach(query::setParameterList);
		if (paging.getPageIndex() != null && paging.getPageSize() != null && paging.getPageIndex() >= 0
		        && paging.getPageSize() >= 0) {
			query.setFirstResult(paging.getPageIndex() * paging.getPageSize());
			query.setMaxResults(paging.getPageSize());
		}
		return query.list();
	}
	
	public StockItemPackagingUOM getStockItemPackagingUOMByUuid(String uuid) {
		return (StockItemPackagingUOM) getSession().createCriteria(StockItemPackagingUOM.class)
		        .add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	public StockItemPackagingUOM saveStockItemPackagingUOM(StockItemPackagingUOM stockItemPackagingUOM) {
		getSession().saveOrUpdate(stockItemPackagingUOM);
		return stockItemPackagingUOM;
	}
	
	public StockBatch getStockBatchByUuid(String uuid) {
		return (StockBatch) getSession().createCriteria(StockBatch.class).add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	public StockBatch saveStockBatch(StockBatch stockBatch) {
		getSession().saveOrUpdate(stockBatch);
		return stockBatch;
	}
	
	public StockItem getStockItemByUuid(String uuid) {
		return (StockItem) getSession().createCriteria(StockItem.class).add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	public List<StockItem> getStockItemsByUuids(List<String> uuids) {
		return getStockItemsByUuids(uuids, false);
	}
	
	public List<StockItem> getStockItemsByUuids(List<String> uuids, boolean includeVoided) {
		if (uuids.isEmpty())
			return new ArrayList<>();
		Criteria query = getSession().createCriteria(StockItem.class).add(Restrictions.in("uuid", uuids));
		if (!includeVoided)
			query.add(Restrictions.eq("voided", false));
		return query.list();
	}
	
	public StockItem saveStockItem(StockItem stockItem) {
		getSession().saveOrUpdate(stockItem);
		return stockItem;
	}
	
	public List<StockItem> searchStockItemCommonName(String text, Boolean isDrugSearch, boolean includeAll, int maxItems) {
		List<String> tokenizedName = Arrays.asList(text.trim().split("\\+"));
		return searchSessionFactory.getSearchSession().search(StockItem.class).where(f -> {
			return f.bool().with(b -> {
				b.minimumShouldMatchNumber(1);
				b.should(f.bool().with(bb -> {
					bb.minimumShouldMatchNumber(1);
					bb.should(f.phrase().field("commonName").matching(text).boost(1.7f));
					bb.should(f.match().field("commonName").matching(text).boost(1.6f));
					bb.should(f.bool().with(bbb -> {
						for (String token : tokenizedName) {
							bbb.must(f.wildcard().field("commonName").matching(token + "*"));
						}
					}).boost(1.3f));
					bb.should(f.match().field("commonName").matching(text).fuzzy(1, 2).boost(1.1f));
				}));
				b.should(f.phrase().field("acronym").matching(text).boost(1.6f));
				if (isDrugSearch != null) {
					b.filter(f.match().field("isDrug").matching(isDrugSearch));
				}
				if (!includeAll) {
					b.filter(f.match().field("voided").matching(false));
				}
			});
		}).fetchHits(maxItems);
	}
	
	public Result<StockItem> findStockItems(StockItemSearch filter) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("select si from stockmanagement.StockItem si");
		
		StringBuilder hqlFilter = new StringBuilder();
		
		if (filter.getUuid() != null && !filter.getUuid().isEmpty()) {
			appendFilter(hqlFilter, "si.uuid = :uuid");
			parameterList.put("uuid", filter.getUuid());
		}
		
		if (filter.getDrugId() != null) {
			appendFilter(hqlFilter, "si.drug.drugId = :drugId");
			parameterList.put("drugId", filter.getDrugId());
		}
		
		if (filter.getConceptId() != null) {
			appendFilter(hqlFilter, "si.concept.conceptId = :conceptId");
			parameterList.put("conceptId", filter.getConceptId());
		}
		
		if (filter.getIsDrug() != null) {
			if (filter.getIsDrug()) {
				appendFilter(hqlFilter, "si.drug.drugId is not null");
			} else {
				appendFilter(hqlFilter, "si.drug.drugId is null");
			}
		}
		
		StringBuilder itemFilter = new StringBuilder();
		if (filter.getStockItemIds() != null && !filter.getStockItemIds().isEmpty()) {
			appendORFilter(itemFilter, "si.id in (:ids)");
			parameterWithList.putIfAbsent("ids", filter.getStockItemIds());
		}
		
		boolean appledDrugConceptsSearch = false;
		if (filter.getSearchEitherDrugsOrConcepts()) {
			if (filter.getDrugs() != null && !filter.getDrugs().isEmpty() && filter.getConcepts() != null
			        && !filter.getConcepts().isEmpty()) {
				appledDrugConceptsSearch = true;
				appendORFilter(itemFilter, "si.drug.drugId in (:drugIds) or si.concept.conceptId in (:conceptIds)");
				parameterWithList.putIfAbsent("drugIds",
				    filter.getDrugs().stream().map(p -> p.getId()).collect(Collectors.toList()));
				parameterWithList.putIfAbsent("conceptIds",
				    filter.getConcepts().stream().map(p -> p.getConceptId()).collect(Collectors.toList()));
			}
		}
		if (!appledDrugConceptsSearch) {
			if (filter.getDrugs() != null && !filter.getDrugs().isEmpty()) {
				appendORFilter(itemFilter, "si.drug.drugId in (:drugIds)");
				parameterWithList.putIfAbsent("drugIds",
				    filter.getDrugs().stream().map(p -> p.getId()).collect(Collectors.toList()));
			}
			if (filter.getConcepts() != null && !filter.getConcepts().isEmpty()) {
				appendORFilter(itemFilter, "si.concept.conceptId in (:conceptIds)");
				parameterWithList.putIfAbsent("conceptIds",
				    filter.getConcepts().stream().map(p -> p.getConceptId()).collect(Collectors.toList()));
			}
		}
		
		if (itemFilter.length() > 0) {
			appendFilter(hqlFilter, itemFilter.toString());
		}
		
		List<Integer> categoryIds = new ArrayList<>();
		if (filter.getCategoryId() != null) {
			categoryIds.add(filter.getCategoryId());
		}
		if (filter.getCategories() != null) {
			categoryIds.addAll(filter.getCategories().stream().map(p -> p.getConceptId()).collect(Collectors.toList()));
		}
		
		if (!categoryIds.isEmpty()) {
			if (categoryIds.size() == 1) {
				appendFilter(hqlFilter, "si.category.conceptId = :categoryId");
				parameterList.put("categoryId", categoryIds.get(0));
			} else {
				appendFilter(hqlFilter, "si.category.conceptId in (:categoryIds)");
				parameterWithList.put("categoryIds", categoryIds.stream().distinct().collect(Collectors.toList()));
			}
		}
		
		if (!filter.getIncludeVoided()) {
			appendFilter(hqlFilter, "si.voided = :vdd");
			parameterList.putIfAbsent("vdd", false);
		}
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		Result<StockItem> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		result.setData(executeEntityQuery(hqlQuery, result, " order by si.id asc", parameterList, parameterWithList));
		loadStockItemDetails(result.getData());
		
		return result;
	}
	
	private void loadStockItemDetails(List<StockItem> items) {
		for (int start = 0; start < items.size(); start += STOCK_ITEM_FETCH_BATCH_SIZE) {
			loadStockItemDetailsBatch(items.subList(start, Math.min(start + STOCK_ITEM_FETCH_BATCH_SIZE, items.size())));
		}
	}
	
	private void loadStockItemDetailsBatch(List<StockItem> items) {
		// Fetch only the selected page. Collection joins in the paged query would apply pagination in memory.
		getSession()
		        .createQuery("select si from stockmanagement.StockItem si"
		                + " left join fetch si.drug left join fetch si.preferredVendor"
		                + " left join fetch si.concept left join fetch si.category left join fetch si.dispensingUnit"
		                + " left join fetch si.purchasePriceUoM pu left join fetch pu.packagingUom"
		                + " left join fetch si.reorderLevelUOM ru left join fetch ru.packagingUom"
		                + " left join fetch si.dispensingUnitPackagingUoM du left join fetch du.packagingUom"
		                + " left join fetch si.defaultStockOperationsUoM su left join fetch su.packagingUom"
		                + " where si.id in (:ids)")
		        .setParameterList("ids", items.stream().map(StockItem::getId).collect(Collectors.toList())).list();
		Set<Integer> conceptIds = new HashSet<>();
		for (StockItem item : items) {
			for (Concept concept : Arrays.asList(item.getConcept(), item.getCategory(), item.getDispensingUnit())) {
				if (concept != null)
					conceptIds.add(concept.getConceptId());
			}
			for (StockItemPackagingUOM unit : Arrays.asList(item.getPurchasePriceUoM(), item.getReorderLevelUOM(),
			    item.getDispensingUnitPackagingUoM(), item.getDefaultStockOperationsUoM())) {
				if (unit != null && unit.getPackagingUom() != null)
					conceptIds.add(unit.getPackagingUom().getConceptId());
			}
		}
		if (!conceptIds.isEmpty()) {
			getSession().createQuery("select distinct c from Concept c left join fetch c.names where c.conceptId in (:ids)")
			        .setParameterList("ids", conceptIds).list();
		}
	}
	
	public Result<StockItem> findStockItemEntities(StockItemSearch filter) {
		DbSession dbSession = getSession();
		Criteria criteria = dbSession.createCriteria(StockItem.class, "si");
		if (filter.getUuid() != null && !filter.getUuid().isEmpty()) {
			criteria.add(Restrictions.eq("si.uuid", filter.getUuid()));
		}
		
		if (filter.getIsDrug() != null) {
			if (filter.getIsDrug()) {
				criteria.add(Restrictions.isNotNull("si.drug"));
			} else {
				criteria.add(Restrictions.isNull("si.drug"));
			}
		}
		
		if (!filter.getIncludeVoided()) {
			criteria.add(Restrictions.eq("si.voided", false));
		}
		
		boolean appledDrugConceptsSearch = false;
		if (filter.getSearchEitherDrugsOrConcepts()) {
			if (filter.getDrugs() != null && !filter.getDrugs().isEmpty() && filter.getConcepts() != null
			        && !filter.getConcepts().isEmpty()) {
				appledDrugConceptsSearch = true;
				criteria.add(Restrictions.or(Restrictions.in("si.drug", filter.getDrugs()),
				    Restrictions.in("si.concept", filter.getConcepts())));
			}
		}
		if (!appledDrugConceptsSearch) {
			if (filter.getDrugs() != null && !filter.getDrugs().isEmpty()) {
				criteria.add(Restrictions.in("si.drug", filter.getDrugs()));
			}
			if (filter.getConcepts() != null && !filter.getConcepts().isEmpty()) {
				criteria.add(Restrictions.in("si.concept", filter.getConcepts()));
			}
		}
		
		Result<StockItem> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		result.setData(executeCriteria(criteria, result));
		
		return result;
	}
	
	public List<StockItem> getExistingStockItemIds(Collection<StockItemSearch.ItemGroupFilter> stockItemFilters) {
		if (stockItemFilters == null || stockItemFilters.isEmpty())
			return new ArrayList<>();
		StringBuilder itemGroupFilters = new StringBuilder();
		HashMap<String, Object> parameterList = new HashMap<>();
		int paramIndex = 0;
		for (StockItemSearch.ItemGroupFilter filter : stockItemFilters) {
			String paramIndexString = Integer.toString(paramIndex);
			StringBuilder itemGroupClause = new StringBuilder();
			if (filter.getDrugId() != null) {
				appendFilter(itemGroupClause, String.format("si.drug.drugId = :drugId%1s", paramIndexString));
				parameterList.put(String.format("drugId%1s", paramIndexString), filter.getDrugId());
			}
			
			if (filter.getConceptId() != null) {
				appendFilter(itemGroupClause, String.format("si.concept.conceptId = :conceptId%1s", paramIndexString));
				parameterList.put(String.format("conceptId%1s", paramIndexString), filter.getConceptId());
			}
			
			if (filter.getIsDrug() != null) {
				if (filter.getIsDrug()) {
					appendFilter(itemGroupClause, "si.drug.drugId is not null");
				} else {
					appendFilter(itemGroupClause, "si.drug.drugId is null");
				}
			}
			if (itemGroupClause.length() > 0) {
				appendORFilter(itemGroupFilters, itemGroupClause.toString());
			}
			
			paramIndex++;
		}
		
		if (parameterList.isEmpty())
			return new ArrayList<>();
		StringBuilder hqlQuery = new StringBuilder("select si from stockmanagement.StockItem si");
		hqlQuery.append(" where ");
		hqlQuery.append(itemGroupFilters.toString());
		
		Result<StockItem> result = new Result<>();
		result.setData(executeEntityQuery(hqlQuery, result, null, parameterList, null));
		return result.getData();
	}
	
	public void voidStockItemPackagingUOM(String uuid, String reason, int voidedBy) {
		DbSession session = getSession();
		Query query = session.createQuery(
		    "UPDATE stockmanagement.StockItemPackagingUOM SET voided=1, dateVoided=:dateVoided, voidedBy=:voidedBy, voidReason=:reason WHERE uuid = :uuid");
		query.setParameter("uuid", uuid);
		query.setDate("dateVoided", new Date());
		query.setInteger("voidedBy", voidedBy);
		query.setString("reason", reason);
		query.executeUpdate();
	}
	
	public void voidStockItemReference(String uuid, String reason, int voidedBy) {
		DbSession session = getSession();
		Query query = session.createQuery(
		    "UPDATE stockmanagement.StockItemReference SET voided=1, dateVoided=:dateVoided, voidedBy=:voidedBy, voidReason=:reason WHERE uuid = :uuid");
		query.setParameter("uuid", uuid);
		query.setDate("dateVoided", new Date());
		query.setInteger("voidedBy", voidedBy);
		query.setString("reason", reason);
		query.executeUpdate();
	}
	
	public Result<StockItemPackagingUOM> findStockItemPackagingUOMs(StockItemSearch filter) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder(
		        "select sipu from stockmanagement.StockItemPackagingUOM sipu join sipu.stockItem si join sipu.packagingUom c");
		
		StringBuilder hqlFilter = new StringBuilder();
		
		if (!StringUtils.isBlank(filter.getUuid())) {
			appendFilter(hqlFilter, "sipu.uuid = :uuid");
			parameterList.putIfAbsent("uuid", filter.getUuid());
		}
		
		if (filter.getStockItemIds() != null && !filter.getStockItemIds().isEmpty()) {
			appendFilter(hqlFilter, "sipu.stockItem.id in (:stockItemId)");
			parameterWithList.putIfAbsent("stockItemId", filter.getStockItemIds());
		}
		
		if (filter.getStockItemUuids() != null && !filter.getStockItemUuids().isEmpty()) {
			appendFilter(hqlFilter, "si.uuid in (:stockItemUuid)");
			parameterWithList.putIfAbsent("stockItemUuid", filter.getStockItemUuids());
		}
		
		if (!filter.getIncludeVoided()) {
			appendFilter(hqlFilter, "sipu.voided = :vdd");
			parameterList.putIfAbsent("vdd", false);
		}
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		Result<StockItemPackagingUOM> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		result.setData(executeEntityQuery(hqlQuery, result, " order by sipu.id", parameterList, parameterWithList));
		
		return result;
	}
	
	public StockBatch findStockBatch(StockItem stockItem, String batchNo, Date expiration) {
		Criteria criteria = getSession().createCriteria(StockBatch.class);
		criteria.add(Restrictions.eq("stockItem", stockItem));
		criteria.add(Restrictions.eq("batchNo", batchNo).ignoreCase());
		if (expiration == null) {
			criteria.add(Restrictions.isNull("expiration"));
		} else {
			criteria.add(Restrictions.eq("expiration", expiration));
		}
		criteria.add(Restrictions.eq("voided", false));
		return (StockBatch) criteria.uniqueResult();
	}
	
	public Result<StockBatch> findStockBatches(StockItemSearch filter) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("select sb from stockmanagement.StockBatch sb join sb.stockItem si");
		
		StringBuilder hqlFilter = new StringBuilder();
		
		if (filter.getStockBatchIds() != null && !filter.getStockBatchIds().isEmpty()) {
			appendFilter(hqlFilter, "sb.id in (:sbIds)");
			parameterWithList.putIfAbsent("sbIds", filter.getStockBatchIds());
		}
		
		if (StringUtils.isNotBlank(filter.getStockBatchUuid())) {
			appendFilter(hqlFilter, "sb.uuid = :sbuuid");
			parameterList.putIfAbsent("sbuuid", filter.getStockBatchUuid());
		}
		
		if (filter.getStockItemId() != null) {
			appendFilter(hqlFilter, "sb.stockItem.id = :stockItemId");
			parameterList.putIfAbsent("stockItemId", filter.getStockItemId());
		}
		
		if (filter.getStockItemUuid() != null) {
			appendFilter(hqlFilter, "si.uuid = :stockItemUuid");
			parameterList.putIfAbsent("stockItemUuid", filter.getStockItemUuid());
		}
		
		if (filter.getExcludeExpired() != null && filter.getExcludeExpired()) {
			appendFilter(hqlFilter, "sb.expiration is null or sb.expiration > :today");
			parameterList.put("today", DateUtil.today());
		}
		
		if (!filter.getIncludeVoided()) {
			appendFilter(hqlFilter, "sb.voided = :vdd");
			parameterList.putIfAbsent("vdd", false);
		}
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		Result<StockBatch> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		
		result.setData(executeEntityQuery(hqlQuery, result, " order by sb.id", parameterList, parameterWithList));
		return result;
	}
	
	public List<StockItemPackagingUOM> getStockItemPackagingUOMsByUuids(List<String> uuids) {
		if (uuids.isEmpty())
			return new ArrayList<>();
		Criteria criteria = getSession().createCriteria(StockItemPackagingUOM.class);
		criteria.add(Restrictions.in("uuid", uuids));
		criteria.add(Restrictions.eq("voided", false));
		return criteria.list();
	}
	
	public List<StockItem> getStockItems(Collection<Integer> stockItemIds) {
		if (stockItemIds == null || stockItemIds.isEmpty())
			return new ArrayList<>();
		DbSession dbSession = getSession();
		Criteria criteria = dbSession.createCriteria(StockItem.class, "s");
		criteria.add(Restrictions.in("s.id", stockItemIds));
		return criteria.list();
	}
	
	public List<StockItemPackagingUOM> getStockItemPackagingUOMs(List<StockItemSearch.PackagingGroup> filters) {
		if (filters == null || filters.isEmpty())
			return new ArrayList<>();
		DbSession dbSession = getSession();
		Criteria criteria = dbSession.createCriteria(StockItemPackagingUOM.class, "s");
		if (filters.size() == 1) {
			StockItemSearch.PackagingGroup filter = filters.get(0);
			criteria.add(Restrictions.and(Restrictions.eq("s.stockItem.id", filter.getStockItemId()),
			    Restrictions.in("s.packagingUom.conceptId", filter.getPackagingUomIds())));
		} else {
			StockItemSearch.PackagingGroup filter0 = filters.get(0);
			StockItemSearch.PackagingGroup filter1 = filters.get(1);
			LogicalExpression orFilter = Restrictions.or(
			    Restrictions.and(Restrictions.eq("s.stockItem.id", filter0.getStockItemId()),
			        Restrictions.in("s.packagingUom.conceptId", filter0.getPackagingUomIds())),
			    Restrictions.and(Restrictions.eq("s.stockItem.id", filter1.getStockItemId()),
			        Restrictions.in("s.packagingUom.conceptId", filter1.getPackagingUomIds())));
			if (filters.size() > 2) {
				for (int i = 2; i < filters.size(); i++) {
					StockItemSearch.PackagingGroup filter = filters.get(i);
					orFilter = Restrictions.or(orFilter,
					    Restrictions.and(Restrictions.eq("s.stockItem.id", filter.getStockItemId()),
					        Restrictions.in("s.packagingUom.conceptId", filter.getPackagingUomIds())));
				}
			}
			criteria.add(orFilter);
		}
		return criteria.list();
	}
	
	public List<StockItem> getStockItemByDrug(Integer drugId) {
		return getSession().createCriteria(StockItem.class).add(Restrictions.eq("drug.drugId", drugId)).list();
	}
	
	public List<StockItem> getStockItemByConcept(Integer conceptId) {
		return getSession().createCriteria(StockItem.class).add(Restrictions.isNull("drug.drugId"))
		        .add(Restrictions.eq("concept.conceptId", conceptId)).list();
	}
	
	public StockItemPackagingUOM getStockItemPackagingUOMByConcept(Integer stockItemId, Integer conceptId) {
		return (StockItemPackagingUOM) getSession().createCriteria(StockItemPackagingUOM.class)
		        .add(Restrictions.eq("stockItem.id", stockItemId)).add(Restrictions.eq("packagingUom.conceptId", conceptId))
		        .addOrder(Order.desc("voided")).setMaxResults(1).uniqueResult();
	}
	
	public StockItemReference getStockItemByReference(StockSource stockSource, String stockReferenceCode) {
		Criteria criteria = getSession().createCriteria(StockItemReference.class);
		criteria.add(Restrictions.eq("referenceSource", stockSource));
		criteria.add(Restrictions.eq("stockReferenceCode", stockReferenceCode));
		return (StockItemReference) criteria.setMaxResults(1).uniqueResult();
	}
	
	public StockItemReference saveStockItemReference(StockItemReference stockItemReference) {
		getSession().saveOrUpdate(stockItemReference);
		return stockItemReference;
	}
	
	public StockItemReference getStockItemReferenceByUuid(String uuid) {
		return (StockItemReference) getSession().createCriteria(StockItemReference.class).add(Restrictions.eq("uuid", uuid))
		        .uniqueResult();
	}
	
	public List<StockItemReference> getStockItemReferenceByStockItem(StockItem stockItem) {
		Criteria criteria = getSession().createCriteria(StockItemReference.class);
		criteria.add(Restrictions.eq("stockItem", stockItem));
		criteria.add(Restrictions.eq("voided", false));
		return criteria.list();
	}
}
