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
public class StockManagementDao extends DaoBase {
	
	public List<LocationTree> getCompleteLocationTree() {
		Criteria criteria = getSession().createCriteria(LocationTree.class);
		return criteria.list();
	}
	
	public void deleteLocation(Integer locationId) {
		
		DbSession session = getSession();
		Query query = session.createQuery("DELETE FROM Location WHERE locationId = :p");
		query.setParameter("p", locationId);
		query.executeUpdate();
	}
	
	public void deleteLocationAttributes(List<Integer> locationAttributeIds) {
		
		DbSession session = getSession();
		Query query = session.createQuery("DELETE FROM LocationAttribute WHERE locationAttributeId in (:p)");
		query.setParameterList("p", locationAttributeIds);
		query.executeUpdate();
	}
	
	public void deleteLocationTreeNodes(Integer locationId) {
		
		DbSession session = getSession();
		Query query = session
		        .createQuery("DELETE FROM stockmanagement.LocationTree WHERE parentLocationId = :p or childLocationId = :p");
		query.setParameter("p", locationId);
		query.executeUpdate();
	}
	
	public void deleteLocationTreeNodes(List<LocationTree> nodes) {
		
		DbSession session = getSession();
		Query query = session.createQuery("DELETE FROM stockmanagement.LocationTree l WHERE l.id in (:p)");
		query.setParameterList("p", nodes.stream().map(p -> p.getId()).collect(Collectors.toList()));
		query.executeUpdate();
	}
	
	public void saveLocationTreeNodes(List<LocationTree> nodes) {
		DbSession session = getSession();
		for (LocationTree locationTree : nodes) {
			session.save(locationTree);
		}
	}
	
	public List<LocationTree> getCompleteLocationTree(Integer atLocationId) {
		Criteria criteria = getSession().createCriteria(LocationTree.class);
		criteria.add(Restrictions.eq("parentLocationId", atLocationId));
		return criteria.list();
	}
	
	public UserRoleScopeLocation getUserRoleScopeLocationByUuid(String uuid) {
		return (UserRoleScopeLocation) getSession().createCriteria(UserRoleScopeLocation.class)
		        .add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	public UserRoleScopeLocation saveUserRoleScopeLocation(UserRoleScopeLocation userRoleScopeLocation) {
		getSession().saveOrUpdate(userRoleScopeLocation);
		return userRoleScopeLocation;
	}
	
	public StockItemTransaction getStockItemTransactionByUuid(String uuid) {
		return (StockItemTransaction) getSession().createCriteria(StockItemTransaction.class)
		        .add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	public StockItemTransaction saveStockItemTransaction(StockItemTransaction stockItemTransaction) {
		getSession().saveOrUpdate(stockItemTransaction);
		return stockItemTransaction;
	}
	
	public StockRule getStockRuleByUuid(String uuid) {
		return (StockRule) getSession().createCriteria(StockRule.class).add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	public StockRule saveStockRule(StockRule stockRule) {
		getSession().saveOrUpdate(stockRule);
		return stockRule;
	}
	
	public StockOperation getStockOperationByUuid(String uuid) {
		return (StockOperation) getSession().createCriteria(StockOperation.class).add(Restrictions.eq("uuid", uuid))
		        .uniqueResult();
	}
	
	public StockOperation saveStockOperation(StockOperation stockOperation) {
		getSession().saveOrUpdate(stockOperation);
		return stockOperation;
	}
	
	public StockItemPackagingUOM getStockItemPackagingUOMByUuid(String uuid) {
		return (StockItemPackagingUOM) getSession().createCriteria(StockItemPackagingUOM.class)
		        .add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	public StockItemPackagingUOM saveStockItemPackagingUOM(StockItemPackagingUOM stockItemPackagingUOM) {
		getSession().saveOrUpdate(stockItemPackagingUOM);
		return stockItemPackagingUOM;
	}
	
	public UserRoleScopeOperationType getUserRoleScopeOperationTypeByUuid(String uuid) {
		return (UserRoleScopeOperationType) getSession().createCriteria(UserRoleScopeOperationType.class)
		        .add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	public UserRoleScopeOperationType saveUserRoleScopeOperationType(UserRoleScopeOperationType userRoleScopeOperationType) {
		getSession().saveOrUpdate(userRoleScopeOperationType);
		return userRoleScopeOperationType;
	}
	
	public UserRoleScope getUserRoleScopeByUuid(String uuid) {
		return (UserRoleScope) getSession().createCriteria(UserRoleScope.class).add(Restrictions.eq("uuid", uuid))
		        .uniqueResult();
	}
	
	public UserRoleScope saveUserRoleScope(UserRoleScope userRoleScope) {
		getSession().saveOrUpdate(userRoleScope);
		return userRoleScope;
	}
	
	public StockOperationItem getStockOperationItemByUuid(String uuid) {
		return (StockOperationItem) getSession().createCriteria(StockOperationItem.class).add(Restrictions.eq("uuid", uuid))
		        .uniqueResult();
	}
	
	public List<StockOperationItem> getStockOperationItemsByStockOperation(Integer stockOperationId) {
		return getSession().createCriteria(StockOperationItem.class)
		        .add(Restrictions.eq("stockOperation.id", stockOperationId)).add(Restrictions.eq("voided", false)).list();
	}
	
	public StockOperationItem saveStockOperationItem(StockOperationItem stockOperationItem) {
		getSession().saveOrUpdate(stockOperationItem);
		return stockOperationItem;
	}
	
	public LocationTree getLocationTreeByUuid(String uuid) {
		return (LocationTree) getSession().createCriteria(LocationTree.class).add(Restrictions.eq("uuid", uuid))
		        .uniqueResult();
	}
	
	public LocationTree saveLocationTree(LocationTree locationTree) {
		getSession().saveOrUpdate(locationTree);
		return locationTree;
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
		if (uuids.isEmpty())
			return new ArrayList<>();
		return getSession().createCriteria(StockItem.class).add(Restrictions.in("uuid", uuids))
		        .add(Restrictions.eq("voided", false)).list();
	}
	
	public StockItem saveStockItem(StockItem stockItem) {
		getSession().saveOrUpdate(stockItem);
		return stockItem;
	}
	
	public StockOperationType getStockOperationTypeByUuid(String uuid) {
		return (StockOperationType) getSession().createCriteria(StockOperationType.class).add(Restrictions.eq("uuid", uuid))
		        .uniqueResult();
	}
	
	public StockOperationType getStockOperationTypeByType(String type) {
		return (StockOperationType) getSession().createCriteria(StockOperationType.class)
		        .add(Restrictions.eq("operationType", type)).uniqueResult();
	}
	
	public List<StockOperationType> getAllStockOperationTypes(boolean includeVoid) {
		Criteria criteria = getSession().createCriteria(StockOperationType.class);
		if (!includeVoid)
			criteria.add(Restrictions.eq("voided", false));
		return criteria.list();
	}
	
	public StockOperationType saveStockOperationType(StockOperationType stockOperationType) {
		getSession().saveOrUpdate(stockOperationType);
		return stockOperationType;
	}
	
	public StockOperationTypeLocationScope getStockOperationTypeLocationScopeByUuid(String uuid) {
		return (StockOperationTypeLocationScope) getSession().createCriteria(StockOperationTypeLocationScope.class)
		        .add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	public StockOperationTypeLocationScope saveStockOperationTypeLocationScope(
	        StockOperationTypeLocationScope stockOperationTypeLocationScope) {
		getSession().saveOrUpdate(stockOperationTypeLocationScope);
		return stockOperationTypeLocationScope;
	}
	
	public Result<UserRoleScope> findUserRoleScopes(UserRoleScopeSearchFilter filter) {
		DbSession dbSession = getSession();
		Criteria criteria = dbSession.createCriteria(UserRoleScope.class, "urs");
		if (isNotNullOrEmpty(filter.getUuid())) {
			criteria.add(Restrictions.eq("urs.uuid", filter.getUuid()));
		}
		
		if (!filter.getIncludeVoided()) {
			criteria.add(Restrictions.eq("urs.voided", false));
		}
		
		if (filter.getUsers() != null && !filter.getUsers().isEmpty()) {
			criteria.add(Restrictions.in("urs.user", filter.getUsers()));
		}
		
		if (filter.getLocation() != null) {
			
			criteria.createAlias("urs.userRoleScopeLocations", "ursl");
			criteria.add(Restrictions.eq("ursl.location", filter.getLocation()));
		}
		
		if (filter.getOperationType() != null) {
			criteria.createAlias("urs.userRoleScopeOperationTypes", "ursot");
			criteria.add(Restrictions.eq("ursot.stockOperationType", filter.getOperationType()));
		}
		
		Result<UserRoleScope> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		result.setData(executeCriteria(criteria, result, Order.desc("urs.dateCreated")));
		return result;
	}
	
	public Result<UserRoleScopeLocation> findUserRoleScopeLocations(UserRoleScopeLocationSearchFilter filter) {
		Criteria criteria = getSession().createCriteria(UserRoleScopeLocation.class);
		if (isNotNullOrEmpty(filter.getUuid())) {
			criteria.add(Restrictions.eq("uuid", filter.getUuid()));
		}
		
		if (!filter.getIncludeVoided()) {
			criteria.add(Restrictions.eq("voided", false));
		}
		
		if (filter.getUserRoleScopes() != null && !filter.getUserRoleScopes().isEmpty()) {
			criteria.add(Restrictions.in("userRoleScope", filter.getUserRoleScopes()));
		}
		
		Result<UserRoleScopeLocation> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		result.setData(executeCriteria(criteria, result, Order.desc("dateCreated")));
		return result;
	}
	
	public Result<UserRoleScopeOperationType> findUserRoleScopeOperationTypeFilters(
	        UserRoleScopeOperationTypeSearchFilter filter) {
		Criteria criteria = getSession().createCriteria(UserRoleScopeOperationType.class);
		if (isNotNullOrEmpty(filter.getUuid())) {
			criteria.add(Restrictions.eq("uuid", filter.getUuid()));
		}
		
		if (!filter.getIncludeVoided()) {
			criteria.add(Restrictions.eq("voided", false));
		}
		
		if (filter.getUserRoleScopes() != null && !filter.getUserRoleScopes().isEmpty()) {
			criteria.add(Restrictions.in("userRoleScope", filter.getUserRoleScopes()));
		}
		
		Result<UserRoleScopeOperationType> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		result.setData(executeCriteria(criteria, result, Order.desc("dateCreated")));
		return result;
	}
	
	public List<StockOperationType> getAllStockOperationTypes() {
		return getSession().createCriteria(StockOperationType.class).list();
	}
	
	public List<StockOperationTypeLocationScope> getAllStockOperationTypeLocationScopes() {
		return getSession().createCriteria(StockOperationTypeLocationScope.class).list();
	}
	
	public void voidUserRoleScopes(List<String> userRoleScopeIds, String reason, int voidedBy) {
		DbSession session = getSession();
		Query query = session.createQuery(
		    "UPDATE stockmanagement.UserRoleScope SET voided=1, dateVoided=:dateVoided, voidedBy=:voidedBy, voidReason=:reason WHERE uuid in (:uuidList)");
		query.setParameterList("uuidList", userRoleScopeIds);
		query.setDate("dateVoided", new Date());
		query.setInteger("voidedBy", voidedBy);
		query.setString("reason", reason);
		query.executeUpdate();
	}
	
	public void voidUserRoleScopeLocations(List<String> userRoleScopeLocationIds, String reason, int voidedBy) {
		DbSession session = getSession();
		Query query = session.createQuery(
		    "UPDATE stockmanagement.UserRoleScopeLocation SET voided=1, dateVoided=:dateVoided, voidedBy=:voidedBy, voidReason=:reason WHERE uuid in (:uuidList)");
		query.setParameterList("uuidList", userRoleScopeLocationIds);
		query.setDate("dateVoided", new Date());
		query.setInteger("voidedBy", voidedBy);
		query.setString("reason", reason);
		query.executeUpdate();
	}
	
	public void voidUserRoleScopeOperationTypes(List<String> userRoleScopeOperationTypeIds, String reason, int voidedBy) {
		DbSession session = getSession();
		Query query = session.createQuery(
		    "UPDATE stockmanagement.UserRoleScopeOperationType SET voided=1, dateVoided=:dateVoided, voidedBy=:voidedBy, voidReason=:reason WHERE uuid in (:uuidList)");
		query.setParameterList("uuidList", userRoleScopeOperationTypeIds);
		query.setDate("dateVoided", new Date());
		query.setInteger("voidedBy", voidedBy);
		query.setString("reason", reason);
		query.executeUpdate();
	}
	
	public List<Integer> searchStockItemCommonName(String text, Boolean isDrugSearch, boolean includeAll, int maxItems) {
		List<String> tokenizedName = Arrays.asList(text.trim().split("\\+"));
		return searchSessionFactory.getSearchSession().search(StockItem.class).select(f -> f.id(Integer.class)).where(f -> {
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
	
	public Result<StockItemDTO> findStockItems(StockItemSearchFilter filter) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("select si.uuid as uuid, si.id as id,\n" + "si.drug.drugId as drugId,\n"
		        + "d.uuid as drugUuid,\n" + "d.name as drugName,\n" + "c.conceptId as conceptId,\n"
		        + "c.uuid as conceptUuid,\n" + "si.hasExpiration as hasExpiration,\n"
		        + "si.preferredVendor.id as preferredVendorId,\n" + "pv.uuid as preferredVendorUuid,\n"
		        + "pv.name as preferredVendorName,\n" + "si.purchasePrice as purchasePrice,\n"
		        + "si.purchasePriceUoM.id as purchasePriceUoMId,\n" + "ppu.uuid as purchasePriceUoMUuid,\n"
		        + "ppu.factor as purchasePriceUoMFactor,\n" + "ppu.packagingUom.conceptId as purchasePriceConceptId,\n"
		        + "si.dispensingUnit.conceptId as dispensingUnitId,\n" + "du.uuid as dispensingUnitUuid,\n"
		        + "si.dispensingUnitPackagingUoM.id as dispensingUnitPackagingUoMId,\n"
		        + "dupu.uuid as dispensingUnitPackagingUoMUuid,\n" + "dupu.factor as dispensingUnitPackagingUoMFactor,\n"
		        + "dupu.packagingUom.conceptId as dispensingUnitPackagingConceptId,\n"
		        + "si.defaultStockOperationsUoM.id as defaultStockOperationsUoMId,\n"
		        + "dsou.uuid as defaultStockOperationsUoMUuid,\n" + "dsou.factor as defaultStockOperationsUoMFactor,\n"
		        + "dsou.packagingUom.conceptId as defaultStockOperationsConceptId,\n" + "si.commonName as commonName,\n"
		        + "si.acronym as acronym,\n" + "si.reorderLevel as reorderLevel,\n"
		        + "si.reorderLevelUOM.id as reorderLevelUoMId,\n" + "rol.uuid as reorderLevelUoMUuid,\n"
		        + "rol.factor as reorderLevelUoMFactor,\n" + "si.category.conceptId as categoryId,\n"
		        + "cg.uuid as categoryUuid,\n" + "rol.packagingUom.conceptId as reorderLevelConceptId,\n"
		        + "si.creator.userId as creator,\n" + "si.dateCreated as dateCreated,\n"
		        + "si.expiryNotice as expiryNotice,\n" + "si.voided as voided\n"
		        + "from stockmanagement.StockItem si left join\n" + " si.drug d left join\n"
		        + "\t si.concept c left join si.preferredVendor pv left join si.purchasePriceUoM ppu left join \n"
		        + "\t si.dispensingUnit du left join si.dispensingUnitPackagingUoM dupu left join \n"
		        + "\t si.defaultStockOperationsUoM dsou left join si.reorderLevelUOM rol left join si.category cg");
		
		StringBuilder hqlFilter = new StringBuilder();
		
		if (isNotNullOrEmpty(filter.getUuid())) {
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
		
		Result<StockItemDTO> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		result.setData(
		    executeQuery(StockItemDTO.class, hqlQuery, result, " order by si.id asc", parameterList, parameterWithList));
		
		if (!result.getData().isEmpty()) {
			List<Integer> conceptNamesToFetch = result.getData().stream()
			        .map(p -> Arrays.asList(p.getConceptId(), p.getDispensingUnitId(), p.getPurchasePriceConceptId(),
			            p.getDispensingUnitPackagingConceptId(), p.getDefaultStockOperationsConceptId(),
			            p.getReorderLevelConceptId(), p.getCategoryId()))
			        .flatMap(Collection::stream).filter(p -> p != null).distinct().collect(Collectors.toList());
			if (!conceptNamesToFetch.isEmpty()) {
				Map<Integer, List<ConceptNameDTO>> conceptNameDTOs = getConceptNamesByConceptIds(conceptNamesToFetch)
				        .stream().collect(Collectors.groupingBy(ConceptNameDTO::getConceptId));
				for (StockItemDTO stockItemDTO : result.getData()) {
					if (stockItemDTO.getConceptId() != null && conceptNameDTOs.containsKey(stockItemDTO.getConceptId())) {
						stockItemDTO.setConceptName(conceptNameDTOs.get(stockItemDTO.getConceptId()).get(0).getName());
					}
					
					if (stockItemDTO.getDispensingUnitId() != null
					        && conceptNameDTOs.containsKey(stockItemDTO.getDispensingUnitId())) {
						stockItemDTO.setDispensingUnitName(
						    conceptNameDTOs.get(stockItemDTO.getDispensingUnitId()).get(0).getName());
					}
					
					if (stockItemDTO.getPurchasePriceConceptId() != null
					        && conceptNameDTOs.containsKey(stockItemDTO.getPurchasePriceConceptId())) {
						stockItemDTO.setPurchasePriceUoMName(
						    conceptNameDTOs.get(stockItemDTO.getPurchasePriceConceptId()).get(0).getName());
					}
					
					if (stockItemDTO.getDispensingUnitPackagingConceptId() != null
					        && conceptNameDTOs.containsKey(stockItemDTO.getDispensingUnitPackagingConceptId())) {
						stockItemDTO.setDispensingUnitPackagingUoMName(
						    conceptNameDTOs.get(stockItemDTO.getDispensingUnitPackagingConceptId()).get(0).getName());
					}
					
					if (stockItemDTO.getDefaultStockOperationsConceptId() != null
					        && conceptNameDTOs.containsKey(stockItemDTO.getDefaultStockOperationsConceptId())) {
						stockItemDTO.setDefaultStockOperationsUoMName(
						    conceptNameDTOs.get(stockItemDTO.getDefaultStockOperationsConceptId()).get(0).getName());
					}
					
					if (stockItemDTO.getReorderLevelConceptId() != null
					        && conceptNameDTOs.containsKey(stockItemDTO.getReorderLevelConceptId())) {
						stockItemDTO.setReorderLevelUoMName(
						    conceptNameDTOs.get(stockItemDTO.getReorderLevelConceptId()).get(0).getName());
					}
					if (stockItemDTO.getCategoryId() != null && conceptNameDTOs.containsKey(stockItemDTO.getCategoryId())) {
						stockItemDTO.setCategoryName(conceptNameDTOs.get(stockItemDTO.getCategoryId()).get(0).getName());
					}
				}
			}
		}
		
		return result;
	}
	
	public Result<StockItem> findStockItemEntities(StockItemSearchFilter filter) {
		DbSession dbSession = getSession();
		Criteria criteria = dbSession.createCriteria(StockItem.class, "si");
		if (isNotNullOrEmpty(filter.getUuid())) {
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
	
	public List<StockItemDTO> getExistingStockItemIds(Collection<StockItemSearchFilter.ItemGroupFilter> stockItemFilters) {
		if (stockItemFilters == null || stockItemFilters.isEmpty())
			return new ArrayList<>();
		StringBuilder itemGroupFilters = new StringBuilder();
		HashMap<String, Object> parameterList = new HashMap<>();
		int paramIndex = 0;
		for (StockItemSearchFilter.ItemGroupFilter filter : stockItemFilters) {
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
		StringBuilder hqlQuery = new StringBuilder(
		        "select si.id as id, si.drug.drugId as drugId, si.concept.conceptId as conceptId from stockmanagement.StockItem si");
		hqlQuery.append(" where ");
		hqlQuery.append(itemGroupFilters.toString());
		
		Result<StockItemDTO> result = new Result<>();
		result.setData(executeQuery(StockItemDTO.class, hqlQuery, result, null, parameterList, null));
		return result.getData();
	}
	
	public Result<StockOperationDTO> findStockOperations(StockOperationSearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("select so.uuid as uuid, so.id as id,\n"
		        + "so.cancelReason as cancelReason,\n" + "so.cancelledBy.userId as cancelledBy,\n"
		        + "so.cancelledDate as cancelledDate,\n" + "so.completedBy.userId as completedBy,\n"
		        + "so.completedDate as completedDate,\n" + "so.submittedBy.userId as submittedBy,\n"
		        + "so.submittedDate as submittedDate,\n" + "so.dispatchedBy.userId as dispatchedBy,\n"
		        + "so.dispatchedDate as dispatchedDate,\n" + "so.returnedBy.userId as returnedBy,\n"
		        + "so.returnedDate as returnedDate,\n" + "so.rejectedBy.userId as rejectedBy,\n"
		        + "so.rejectedDate as rejectedDate,\n" + "dest.uuid as destinationUuid,\n"
		        + "coalesce(destl.name,dests.name) as destinationName,\n" + "so.externalReference as externalReference,\n"
		        + "loc.uuid as atLocationUuid,\n" + "loc.name as atLocationName,\n" + "so.operationDate as operationDate,\n"
		        + "so.locked as locked,\n" + "so.operationNumber as operationNumber,\n"
		        + "so.operationOrder as operationOrder,\n" + "so.remarks as remarks,\n" + "sorce.uuid as sourceUuid,\n"
		        + "coalesce(sorcel.name,sorces.name) as sourceName,\n" + "so.status as status,\n"
		        + "so.returnReason as returnReason,\n" + "so.rejectionReason as rejectionReason,\n"
		        + "sot.uuid as operationTypeUuid,\n" + "sot.operationType as operationType,\n"
		        + "sot.name as operationTypeName,\n" + "rp.userId as responsiblePerson,\n"
		        + "rp.uuid as responsiblePersonUuid,\n" + "so.responsiblePersonOther as responsiblePersonOther,\n"
		        + "so.creator.userId as creator,\n" + "so.dateCreated as dateCreated,\n" + "rsn.uuid as reasonUuid,\n"
		        + "rsn.conceptId as reasonId,\n" + "so.approvalRequired as approvalRequired,\n" + "so.voided as voided\n"
		        + "from stockmanagement.StockOperation so inner join\n" + "     so.stockOperationType sot left join\n"
		        + "\t so.destination dest left join dest.location destl left join dest.stockSource dests left join\n"
		        + "\t so.source sorce left join sorce.location sorcel left join sorce.stockSource sorces left join\n"
		        + "\t so.responsiblePerson rp left join\n" + "\t so.atLocation loc left join\n" + "\t so.reason rsn");
		StringBuilder hqlFilter = new StringBuilder();
		
		if (StringUtils.isNotBlank(filter.getStockOperationUuid())) {
			appendFilter(hqlFilter, "so.uuid = :stockOperationUuid");
			parameterList.putIfAbsent("stockOperationUuid", filter.getStockOperationUuid());
		}
		if (filter.getLocationId() != null) {
			appendFilter(hqlFilter, "(destl.locationId = :locationId OR sorcel.locationId = :locationId )");
			parameterList.putIfAbsent("locationId", filter.getLocationId());
		}
		
		if (filter.getPartyId() != null) {
			appendFilter(hqlFilter, "(so.source.id = :partyId OR so.destination.id = :partyId )");
			parameterList.putIfAbsent("partyId", filter.getPartyId());
		}
		
		if (filter.getOperationTypeId() != null && filter.getOperationTypeId().size() > 0) {
			if (filter.getOperationTypeId().size() == 1) {
				appendFilter(hqlFilter, "so.stockOperationType.id = :otid");
				parameterList.putIfAbsent("otid", filter.getOperationTypeId().get(0));
			} else {
				appendFilter(hqlFilter, "so.stockOperationType.id in (:otid)");
				parameterWithList.putIfAbsent("otid", filter.getOperationTypeId());
			}
		}
		
		if (filter.getStatus() != null && filter.getStatus().size() > 0) {
			if (filter.getStatus().size() == 1) {
				appendFilter(hqlFilter, "so.status = :status");
				parameterList.putIfAbsent("status", filter.getStatus().get(0));
			} else {
				appendFilter(hqlFilter, "so.status in (:status)");
				parameterWithList.putIfAbsent("status", filter.getStatus());
			}
		}
		
		if (filter.getOperationDateMin() != null) {
			appendFilter(hqlFilter, "so.operationDate >= :odm");
			parameterList.putIfAbsent("odm", filter.getOperationDateMin());
		}
		
		if (filter.getOperationDateMax() != null) {
			appendFilter(hqlFilter, "so.operationDate <= :odmx");
			parameterList.putIfAbsent("odmx", filter.getOperationDateMax());
		}
		
		if (StringUtils.isNotBlank(filter.getOperationNumber())) {
			String operationNumber = filter.getOperationNumber().replaceAll("%", "");
			if (operationNumber.length() == 0)
				return new Result<>(new ArrayList<>(), 0);
			operationNumber = operationNumber.toUpperCase() + "%";
			appendFilter(hqlFilter, "so.operationNumber like :operationNumber");
			parameterList.putIfAbsent("operationNumber", operationNumber);
		}
		
		if (filter.getIsLocationOther() != null && filter.getIsLocationOther()) {
			appendFilter(hqlFilter, "dest.stockSource.conceptId is not null or sorce.stockSource.conceptId is not null");
		}
		
		if (filter.getSourceTypeIds() != null && filter.getSourceTypeIds().size() > 0) {
			if (filter.getSourceTypeIds().size() == 1) {
				appendFilter(hqlFilter,
				    "dests.sourceType.conceptId = :sourceTypes or sorces.sourceType.conceptId = :sourceTypes");
				parameterList.putIfAbsent("sourceTypes", filter.getSourceTypeIds().get(0));
			} else {
				appendFilter(hqlFilter,
				    "dests.sourceType.conceptId in (:sourceTypes) or sorces.sourceType.conceptId in (:sourceTypes)");
				parameterWithList.putIfAbsent("sourceTypes", filter.getSourceTypeIds());
			}
		}
		
		if (!StringUtils.isBlank(filter.getSearchText())) {
			String q = filter.getSearchText().replaceAll("%", "");
			if (q.length() == 0)
				return new Result<>(new ArrayList<>(), 0);
			PartySearchFilter partySearchFilter = new PartySearchFilter();
			partySearchFilter.setSearchText(q);
			Result<PartyDTO> partyList = findParty(partySearchFilter);
			List<Integer> partyIds = null;
			if (!partyList.getData().isEmpty()) {
				partyIds = partyList.getData().stream().map(p -> p.getId()).limit(30).collect(Collectors.toList());
			}
			q = q + "%";
			if (partyIds != null && partyIds.size() > 0) {
				appendFilter(hqlFilter,
				    "(so.source.id in (:partyList) OR so.destination.id in (:partyList)) OR so.operationNumber like :qtxt");
				parameterWithList.putIfAbsent("partyList", partyIds);
			} else {
				appendFilter(hqlFilter, "so.operationNumber like :qtxt");
			}
			parameterList.putIfAbsent("qtxt", q);
		}
		
		if (!filter.getIncludeVoided()) {
			appendFilter(hqlFilter, "so.voided = :vdd");
			parameterList.putIfAbsent("vdd", false);
		}
		
		if (filter.getStockItemId() != null) {
			appendFilter(hqlFilter,
			    " exists ( from stockmanagement.StockOperationItem soi where soi.stockOperation.id = so.id and soi.stockItem.id = :soiid and soi.voided = 0 )");
			parameterList.putIfAbsent("soiid", filter.getStockItemId());
		}
		
		if (recordPrivilegeFilters != null) {
			PartySearchFilter partySearchFilter = new PartySearchFilter();
			partySearchFilter.setIncludeVoided(true);
			partySearchFilter.setLocationIds(
			    recordPrivilegeFilters.stream().map(p -> p.getLocationId()).collect(Collectors.toList()));
			List<PartyDTO> partyDTOs = findParty(partySearchFilter).getData().stream().collect(Collectors.toList());
			
			int paramIndex = 0;
			StringBuilder recordPrivileges = new StringBuilder();
			for (PartyDTO party : partyDTOs) {
				appendORFilter(recordPrivileges,
				    String.format(
				        " (dest.id = :rpfl%1$s OR sorce.id = :rpfl%1$s ) AND so.stockOperationType.id in (:rpft%1$s)",
				        paramIndex));
				parameterList.putIfAbsent("rpfl" + Integer.toString(paramIndex), party.getId());
				parameterWithList.putIfAbsent("rpft" + Integer.toString(paramIndex),
				    recordPrivilegeFilters.stream().filter(p -> p.getLocationId().equals(party.getLocationId()))
				            .map(p -> p.getOperationTypeId()).collect(Collectors.toList()));
				paramIndex++;
			}
			
			appendFilter(hqlFilter, recordPrivileges.toString());
		}
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		Result<StockOperationDTO> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		result.setData(executeQuery(StockOperationDTO.class, hqlQuery, result, " order by so.id desc", parameterList,
		    parameterWithList));
		
		if (!result.getData().isEmpty()) {
			List<StockOperationDTO> operationsWithReason = result.getData().stream().filter(p -> p.getReasonId() != null)
			        .collect(Collectors.toList());
			if (!operationsWithReason.isEmpty()) {
				List<ConceptNameDTO> conceptNameDTOs = getConceptNamesByConceptIds(
				    operationsWithReason.stream().map(p -> p.getReasonId()).collect(Collectors.toList()));
				for (StockOperationDTO stockOperationDTO : operationsWithReason) {
					Optional<ConceptNameDTO> conceptNameDTO = conceptNameDTOs.stream()
					        .filter(p -> p.getConceptId().equals(stockOperationDTO.getReasonId())).findFirst();
					if (conceptNameDTO.isPresent()) {
						stockOperationDTO.setReasonName(conceptNameDTO.get().getName());
					}
				}
			}
		}
		
		return result;
	}
	
	public Result<PartyDTO> findParty(PartySearchFilter filter) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder(
		        "select p.id as id, p.uuid as uuid,\n" + "coalesce(l.name, s.name) as name,\n" + "s.acronym as acronym,\n"
		                + "l.uuid as locationUuid, l.locationId as locationId,\n"
		                + "s.uuid as stockSourceUuid, s.id as stockSourceId,\n" + "p.voided as voided\n"
		                + "from stockmanagement.Party p left join\n" + "\t p.location l left join p.stockSource s");
		
		StringBuilder hqlFilter = new StringBuilder();
		
		if (filter.getPartyIds() != null && !filter.getPartyIds().isEmpty()) {
			appendFilter(hqlFilter, "p.id in (:pids)");
			parameterWithList.putIfAbsent("pids", filter.getPartyIds());
		}
		
		if (filter.getPartyUuids() != null && !filter.getPartyUuids().isEmpty()) {
			appendFilter(hqlFilter, "p.uuid in (:puuids)");
			parameterWithList.putIfAbsent("puuids", filter.getPartyUuids());
		}
		
		if (filter.getLocationIds() != null && !filter.getLocationIds().isEmpty()) {
			appendFilter(hqlFilter, "l.id in (:lids)");
			parameterWithList.putIfAbsent("lids", filter.getLocationIds());
		}
		
		if (filter.getLocationUuids() != null && !filter.getLocationUuids().isEmpty()) {
			appendFilter(hqlFilter, "l.uuid in (:luuids)");
			parameterWithList.putIfAbsent("luuids", filter.getLocationUuids());
		}
		
		if (StringUtils.isNotBlank(filter.getSearchText())) {
			String q = filter.getSearchText().replaceAll("%", "");
			if (q.length() == 0)
				return new Result<>(new ArrayList<>(), 0);
			q = "%" + q + "%";
			appendFilter(hqlFilter, "lower(l.name) like lower(:name) or lower(s.name) like lower(:name)");
			parameterList.putIfAbsent("name", q);
		}
		
		if (filter.getIncludeVoided() == null || !filter.getIncludeVoided()) {
			appendFilter(hqlFilter, "p.voided = :vdd");
			parameterList.putIfAbsent("vdd", false);
		}
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		Result<PartyDTO> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		result.setData(
		    executeQuery(PartyDTO.class, hqlQuery, result, " order by name asc", parameterList, parameterWithList));
		return result;
	}
	
	public Map<String, String> getLocationNamesByUuid(List<String> locationUuids) {
		if (locationUuids == null || locationUuids.isEmpty())
			return new HashMap<>();
		Query query = getSession()
		        .createQuery("select l.uuid as uuid, l.name as name from Location l where l.uuid in (:uuids)")
		        .setParameterList("uuids", locationUuids);
		List result = query.list();
		Map<String, String> resultMap = new HashMap<>();
		for (Object object : result) {
			resultMap.putIfAbsent((String) (((Object[]) object)[0]), (String) (((Object[]) object)[1]));
		}
		return resultMap;
	}
	
	public Map<Integer, String> getLocationNames(List<Integer> locationIds) {
		if (locationIds == null || locationIds.isEmpty())
			return new HashMap<>();
		Query query = getSession()
		        .createQuery(
		            "select l.locationId as locationId, l.name as name from Location l where l.locationId in (:ids)")
		        .setParameterList("ids", locationIds);
		List result = query.list();
		Map<Integer, String> resultMap = new HashMap<>();
		for (Object object : result) {
			resultMap.putIfAbsent(Integer.valueOf(((Number) (((Object[]) object)[0])).intValue()),
			    (String) (((Object[]) object)[1]));
		}
		return resultMap;
	}
	
	public List<UserPersonNameDTO> getPersonNameByUserIds(List<Integer> ids) {
		if (ids == null || ids.isEmpty())
			return new ArrayList<>();
		Query query = sessionFactory.getCurrentSession().createQuery(
		    "select u.uuid as uuid, u.userId as userId, p.givenName as givenName, p.middleName as middleName, p.familyName as familyName from User u join u.person up join up.names p where u.userId in (:ids)")
		        .setParameterList("ids", ids);
		query = query.setResultTransformer(new AliasToBeanResultTransformer(UserPersonNameDTO.class));
		return query.list();
	}
	
	public StockSource getStockSourceByUuid(String uuid) {
		return (StockSource) getSession().createCriteria(StockSource.class).add(Restrictions.eq("uuid", uuid))
		        .uniqueResult();
	}
	
	public StockSource saveStockSource(StockSource stockSource) {
		getSession().saveOrUpdate(stockSource);
		return stockSource;
	}
	
	public Result<StockSource> findStockSources(StockSourceSearchFilter filter) {
		DbSession dbSession = getSession();
		Criteria criteria = dbSession.createCriteria(StockSource.class, "ss");
		if (isNotNullOrEmpty(filter.getUuid())) {
			criteria.add(Restrictions.eq("ss.uuid", filter.getUuid()));
		}
		
		if (filter.getSourceType() != null) {
			criteria.add(Restrictions.eq("ss.sourceType", filter.getSourceType()));
		}
		
		if (!filter.getIncludeVoided()) {
			criteria.add(Restrictions.eq("ss.voided", false));
		}
		
		if (StringUtils.isNotBlank(filter.getTextSearch())) {
			String textSearch = filter.getTextSearch().replaceAll("%", "");
			if (textSearch.length() == 0)
				return new Result<>(new ArrayList<>(), 0);
			textSearch = textSearch + "%";
			criteria.add(
			    Restrictions.or(Restrictions.ilike("ss.name", textSearch), Restrictions.ilike("ss.acronym", textSearch)));
		}
		
		Result<StockSource> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		result.setData(executeCriteria(criteria, result));
		
		return result;
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
	
	public void voidStockSources(List<String> stockSourceIds, String reason, int voidedBy) {
		DbSession session = getSession();
		Query query = session.createQuery(
		    "UPDATE stockmanagement.StockSource SET voided=1, dateVoided=:dateVoided, voidedBy=:voidedBy, voidReason=:reason WHERE uuid in (:uuidList)");
		query.setParameterList("uuidList", stockSourceIds);
		query.setDate("dateVoided", new Date());
		query.setInteger("voidedBy", voidedBy);
		query.setString("reason", reason);
		query.executeUpdate();
	}
	
	public Party getPartyByUuid(String uuid) {
		return (Party) getSession().createCriteria(Party.class).add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	public Party getPartyById(Integer id) {
		return (Party) getSession().createCriteria(Party.class).add(Restrictions.eq("id", id)).uniqueResult();
	}
	
	public Party getPartyByStockSource(StockSource stockSource) {
		if (stockSource == null)
			return null;
		Criteria criteria = getSession().createCriteria(Party.class);
		criteria.add(Restrictions.eq("stockSource", stockSource));
		return (Party) criteria.uniqueResult();
	}
	
	public void deleteParty(Party party) {
		DbSession session = getSession();
		Query query = session.createQuery("DELETE stockmanagement.Party WHERE id = :id");
		query.setParameter("id", party.getId());
		query.executeUpdate();
	}
	
	public Party getPartyByLocation(Location location) {
		if (location == null)
			return null;
		Criteria criteria = getSession().createCriteria(Party.class);
		criteria.add(Restrictions.eq("location", location));
		return (Party) criteria.uniqueResult();
	}
	
	public List<Party> getPartyListByLocations(Collection<Location> locations) {
		if (locations == null || locations.isEmpty())
			return new ArrayList<>();
		Criteria criteria = getSession().createCriteria(Party.class);
		criteria.add(Restrictions.in("location", locations));
		return criteria.list();
	}
	
	public Party saveParty(Party party) {
		getSession().saveOrUpdate(party);
		return party;
	}
	
	public List<Party> findParty(Boolean hasLocation, Boolean hasStockSource) {
		Criteria criteria = getSession().createCriteria(Party.class);
		if (hasLocation != null) {
			if (hasLocation.booleanValue()) {
				criteria.add(Restrictions.isNotNull("location"));
			} else {
				criteria.add(Restrictions.isNull("location"));
			}
		}
		
		if (hasStockSource != null) {
			if (hasStockSource.booleanValue()) {
				criteria.add(Restrictions.isNotNull("stockSource"));
			} else {
				criteria.add(Restrictions.isNull("stockSource"));
			}
		}
		
		return criteria.list();
	}
	
	public List<Integer> getActiveUsersAssignedForScope(Integer locationId, List<String> roles) {
		if (locationId == null || roles == null || roles.size() == 0)
			return new ArrayList<>();
		
		StringBuilder hqlQuery = new StringBuilder("SELECT DISTINCT urs.user.userId as userId "
		        + "FROM stockmanagement.UserRoleScope urs join \n" + " urs.userRoleScopeLocations ursl join urs.user u, \n"
		        + " stockmanagement.LocationTree lt, \n" + " Location l \n"
		        + "WHERE urs.role.role in (:roles) AND ursl.enableDescendants = 1 AND ursl.location.id = lt.parentLocationId AND lt.childLocationId = l.id AND "
		        + "lt.childLocationId=:locid AND urs.enabled = 1 AND urs.voided = 0 AND "
		        + "(urs.permanent = 1 OR (urs.activeFrom <= :from AND urs.activeTo >= :to )) AND "
		        + "ursl.voided=0 AND u.retired=0 AND l.retired=0 ");
		
		Query query = getSession().createQuery(hqlQuery.toString());
		query.setParameter("locid", locationId);
		query.setParameterList("roles", roles);
		query.setParameter("from", DateUtil.today());
		query.setParameter("to", DateUtil.today());
		
		List<Integer> result = new ArrayList<>();
		List partialResult = query.list();
		for (Iterator it = partialResult.iterator(); it.hasNext();) {
			Object row = it.next();
			Integer integer = ((Number) row).intValue();
			result.add(integer);
		}
		
		hqlQuery = new StringBuilder("SELECT DISTINCT urs.user.userId as userId "
		        + "FROM stockmanagement.UserRoleScope urs join \n"
		        + "     urs.userRoleScopeLocations ursl join urs.user u join \n" + "     ursl.location l \n"
		        + "WHERE urs.role.role in (:roles) AND l.locationId = :locid AND ursl.enableDescendants = 0 AND urs.enabled = 1 AND urs.voided = 0 AND "
		        + "(urs.permanent = 1 OR (urs.activeFrom <= :from AND urs.activeTo >= :to )) and ursl.voided=0 "
		        + " AND u.retired=0 AND l.retired=0");
		
		query = getSession().createQuery(hqlQuery.toString());
		query.setParameter("locid", locationId);
		query.setParameterList("roles", roles);
		query.setParameter("from", DateUtil.today());
		query.setParameter("to", DateUtil.today());
		partialResult = query.list();
		for (Iterator it = partialResult.iterator(); it.hasNext();) {
			Object row = it.next();
			Integer integer = ((Number) row).intValue();
			result.add(integer);
		}
		return result;
		
	}
	
	public List<PartyDTO> getAllParties() {
		StringBuilder hqlQuery = new StringBuilder(
		        "SELECT p.uuid as uuid, l.uuid as locationUuid, ss.uuid as stockSourceUuid,"
		                + " coalesce(l.name, ss.name) as name, ss.acronym as acronym\n"
		                + " FROM stockmanagement.Party p left join\n" + "   p.location l left join\n"
		                + "   p.stockSource ss");
		Query query = getSession().createQuery(hqlQuery.toString());
		query = query.setResultTransformer(new AliasToBeanResultTransformer(PartyDTO.class));
		return query.list();
	}
	
	public Result<StockItemPackagingUOMDTO> findStockItemPackagingUOMs(StockItemPackagingUOMSearchFilter filter) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("select sipu.id as id,\n" + "\tsipu.uuid as uuid,\n"
		        + "\tsipu.voided as voided,\n" + "\tsipu.factor as factor,\n" + "\tc.conceptId as packagingUomId,\n"
		        + "\tc.uuid as packagingUomUuid,\n" + "\tsipu.stockItem.id as stockItemId,\n"
		        + "\tsi.uuid as stockItemUuid,\n"
		        + (filter.includingDispensingUnit() ? "\tdu.conceptId as stockItemDispensingUnitId,\n" : "")
		        + "CASE sipu.id WHEN si.defaultStockOperationsUoM.id THEN true ELSE false END as isDefaultStockOperationsUoM,\n"
		        + "CASE sipu.id WHEN si.dispensingUnitPackagingUoM.id THEN true ELSE false END as isDispensingUnit\n"
		        + "from stockmanagement.StockItemPackagingUOM sipu join\n" + "  sipu.stockItem si join \n"
		        + "  sipu.packagingUom c \n" + (filter.includingDispensingUnit() ? " left join si.dispensingUnit du" : ""));
		
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
		
		Result<StockItemPackagingUOMDTO> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		result.setData(executeQuery(StockItemPackagingUOMDTO.class, hqlQuery, result, " order by sipu.id", parameterList,
		    parameterWithList));
		
		if (!result.getData().isEmpty()) {
			List<Integer> conceptIdsToFetch = result.getData().stream().map(p -> p.getPackagingUomId())
			        .collect(Collectors.toList());
			if (filter.includingDispensingUnit()) {
				conceptIdsToFetch.addAll(result.getData().stream().map(p -> p.getStockItemDispensingUnitId())
				        .filter(p -> p != null).collect(Collectors.toList()));
			}
			List<ConceptNameDTO> conceptNameDTOs = getConceptNamesByConceptIds(conceptIdsToFetch);
			for (StockItemPackagingUOMDTO stockItemPackagingUOMDTO : result.getData()) {
				Optional<ConceptNameDTO> conceptNameDTO = conceptNameDTOs.stream()
				        .filter(p -> p.getConceptId().equals(stockItemPackagingUOMDTO.getPackagingUomId())
				                && p.getLocale().getDisplayName().equals(Context.getLocale().getDisplayName()))
				        .findFirst();
				if (conceptNameDTO.isPresent()) {
					stockItemPackagingUOMDTO.setPackagingUomName(conceptNameDTO.get().getName());
				} else {
					conceptNameDTO = conceptNameDTOs.stream()
					        .filter(p -> p.getConceptId().equals(stockItemPackagingUOMDTO.getPackagingUomId())).findFirst();
					if (conceptNameDTO.isPresent()) {
						stockItemPackagingUOMDTO.setPackagingUomName(conceptNameDTO.get().getName());
					}
				}
				if (filter.includingDispensingUnit() && stockItemPackagingUOMDTO.getStockItemDispensingUnitId() != null) {
					conceptNameDTO = conceptNameDTOs.stream()
					        .filter(p -> p.getConceptId().equals(stockItemPackagingUOMDTO.getStockItemDispensingUnitId()))
					        .findFirst();
					if (conceptNameDTO.isPresent()) {
						stockItemPackagingUOMDTO.setStockItemDispensingUnitName(conceptNameDTO.get().getName());
					}
				}
			}
		}
		
		return result;
	}
	
	public Result<StockOperationItemDTO> findStockOperationItems(StockOperationItemSearchFilter filter) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("SELECT soi.id as id,\n" + "soi.uuid as uuid,\n"
		        + "soi.stockItem.id as stockItemId,\n" + "si.uuid as stockItemUuid,\n"
		        + "si.drug.drugId as stockItemDrugId,\n" + "si.concept.conceptId as stockItemConceptId,\n"
		        + "sipu.uuid as stockItemPackagingUOMUuid,\n" + "sipu.packagingUom.conceptId as packagingUoMId,\n"
		        + "qrpu.uuid as quantityReceivedPackagingUOMUuid,\n"
		        + "qrpu.packagingUom.conceptId as quantityReceivedPackagingUOMUoMId,\n"
		        + "qrqpu.uuid as quantityRequestedPackagingUOMUuid,\n"
		        + "qrqpu.packagingUom.conceptId as quantityRequestedPackagingUOMUoMId,\n" + "sb.id as stockBatchId,\n"
		        + "sb.uuid as stockBatchUuid,\n" + "sb.batchNo as batchNo,\n" + "sb.expiration as expiration,\n"
		        + "so.id as stockOperationId,\n" + "so.uuid as stockOperationUuid,\n" + "soi.quantity as quantity,\n"
		        + "soi.quantityReceived as quantityReceived,\n" + "soi.quantityRequested as quantityRequested,\n"
		        + "soi.purchasePrice as purchasePrice,\n" + "si.hasExpiration as hasExpiration,\n"
		        + "si.commonName as commonName,\n" + "si.acronym as acronym\n"
		        + "from stockmanagement.StockOperationItem soi join\n" + "\t soi.stockItem si left join\n"
		        + " soi.stockBatch sb left join\n" + " soi.stockItemPackagingUOM sipu left join\n"
		        + " soi.stockOperation so left join soi.quantityReceivedPackagingUOM qrpu left join soi.quantityRequestedPackagingUOM qrqpu");
		
		StringBuilder hqlFilter = new StringBuilder();
		
		if (filter.getUuid() != null) {
			appendFilter(hqlFilter, "soi.uuid = :uuid");
			parameterList.putIfAbsent("uuid", filter.getUuid());
		}
		
		if (filter.getStockItemId() != null) {
			appendFilter(hqlFilter, "si.id = :stockItemId");
			parameterList.putIfAbsent("stockItemId", filter.getStockItemId());
		}
		
		if (filter.getStockItemUuid() != null) {
			appendFilter(hqlFilter, "si.uuid = :stockItemUuid");
			parameterList.putIfAbsent("stockItemUuid", filter.getStockItemUuid());
		}
		
		if (!filter.getIncludeVoided()) {
			appendFilter(hqlFilter, "soi.voided = :vdd");
			parameterList.putIfAbsent("vdd", false);
		}
		
		if (filter.getStockOperationIds() != null && !filter.getStockOperationIds().isEmpty()) {
			appendFilter(hqlFilter, "so.id in (:opids)");
			parameterWithList.putIfAbsent("opids", filter.getStockOperationIds());
		}
		
		if (filter.getStockOperationUuids() != null && !filter.getStockOperationUuids().isEmpty()) {
			appendFilter(hqlFilter, "so.uuid in (:opuuids)");
			parameterWithList.putIfAbsent("opuuids", filter.getStockOperationUuids());
		}
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		Result<StockOperationItemDTO> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		result.setData(executeQuery(StockOperationItemDTO.class, hqlQuery, result, " order by soi.id", parameterList,
		    parameterWithList));
		
		if (!result.getData().isEmpty()) {
			List<Integer> conceptIds = new ArrayList<>();
			List<Integer> drugIds = new ArrayList<>();
			if (filter.getIncludePackagingUnitName()) {
				conceptIds.addAll(result.getData().stream().filter(p -> p.getPackagingUoMId() != null)
				        .map(p -> p.getPackagingUoMId()).collect(Collectors.toList()));
				conceptIds.addAll(result.getData().stream().filter(p -> p.getQuantityReceivedPackagingUOMUoMId() != null)
				        .map(p -> p.getQuantityReceivedPackagingUOMUoMId()).collect(Collectors.toList()));
				conceptIds.addAll(result.getData().stream().filter(p -> p.getQuantityRequestedPackagingUOMUoMId() != null)
				        .map(p -> p.getQuantityRequestedPackagingUOMUoMId()).collect(Collectors.toList()));
			}
			if (filter.getIncludeStockUnitName()) {
				conceptIds.addAll(result.getData().stream().filter(p -> p.getStockItemConceptId() != null)
				        .map(p -> p.getStockItemConceptId()).collect(Collectors.toList()));
				drugIds.addAll(result.getData().stream().filter(p -> p.getStockItemDrugId() != null)
				        .map(p -> p.getStockItemDrugId()).collect(Collectors.toList()));
			}
			
			if (!conceptIds.isEmpty() || !drugIds.isEmpty()) {
				List<ConceptNameDTO> conceptNameDTOs = conceptIds.isEmpty() ? new ArrayList<>()
				        : getConceptNamesByConceptIds(conceptIds);
				List<ConceptNameDTO> drugNames = drugIds.isEmpty() ? new ArrayList<>() : getDrugNamesByDrugIds(drugIds);
				for (StockOperationItemDTO stockOperationItemDTO : result.getData()) {
					
					if (filter.getIncludePackagingUnitName()) {
						Optional<ConceptNameDTO> conceptNameDTO = null;
						if (stockOperationItemDTO.getPackagingUoMId() != null
						        && stockOperationItemDTO.getStockItemPackagingUOMUuid() != null) {
							conceptNameDTO = conceptNameDTOs.stream()
							        .filter(p -> p.getConceptId().equals(stockOperationItemDTO.getPackagingUoMId()))
							        .findFirst();
							if (conceptNameDTO.isPresent()) {
								stockOperationItemDTO.setStockItemPackagingUOMName(conceptNameDTO.get().getName());
							}
							
							BigDecimal factor = getStockItemPackagingUOMByUuid(
							    stockOperationItemDTO.getStockItemPackagingUOMUuid()).getFactor();
							
							stockOperationItemDTO.setStockItemPackagingUOMFactor(factor);
						}
						
						if (stockOperationItemDTO.getQuantityReceivedPackagingUOMUoMId() != null
						        && stockOperationItemDTO.getQuantityReceivedPackagingUOMUuid() != null) {
							conceptNameDTO = conceptNameDTOs.stream().filter(
							    p -> p.getConceptId().equals(stockOperationItemDTO.getQuantityReceivedPackagingUOMUoMId()))
							        .findFirst();
							if (conceptNameDTO.isPresent()) {
								stockOperationItemDTO.setQuantityReceivedPackagingUOMName(conceptNameDTO.get().getName());
							}
							BigDecimal factor = getStockItemPackagingUOMByUuid(
							    stockOperationItemDTO.getQuantityReceivedPackagingUOMUuid()).getFactor();
							stockOperationItemDTO.setQuantityReceivedPackagingUOMFactor(factor);
						}
						
						if (stockOperationItemDTO.getQuantityRequestedPackagingUOMUoMId() != null
						        && stockOperationItemDTO.getQuantityRequestedPackagingUOMUuid() != null) {
							conceptNameDTO = conceptNameDTOs.stream().filter(
							    p -> p.getConceptId().equals(stockOperationItemDTO.getQuantityRequestedPackagingUOMUoMId()))
							        .findFirst();
							if (conceptNameDTO.isPresent()) {
								stockOperationItemDTO.setQuantityRequestedPackagingUOMName(conceptNameDTO.get().getName());
							}
							BigDecimal factor = getStockItemPackagingUOMByUuid(
							    stockOperationItemDTO.getQuantityRequestedPackagingUOMUuid()).getFactor();
							stockOperationItemDTO.setQuantityRequestedPackagingUOMFactor(factor);
						}
					}
					if (filter.getIncludeStockUnitName()) {
						String conceptName = null;
						String drugName = null;
						
						if (stockOperationItemDTO.getStockItemConceptId() != null) {
							Optional<ConceptNameDTO> conceptNameDTO = conceptNameDTOs.stream()
							        .filter(p -> p.getConceptId().equals(stockOperationItemDTO.getStockItemConceptId()))
							        .findFirst();
							if (conceptNameDTO.isPresent()) {
								conceptName = conceptNameDTO.get().getName();
							}
						}
						
						if (stockOperationItemDTO.getStockItemDrugId() != null) {
							Optional<ConceptNameDTO> conceptNameDTO = drugNames.stream()
							        .filter(p -> p.getConceptId().equals(stockOperationItemDTO.getStockItemDrugId()))
							        .findFirst();
							if (conceptNameDTO.isPresent()) {
								drugName = conceptNameDTO.get().getName();
							}
						}
						
						if (drugName != null && StringUtils.isNotBlank(stockOperationItemDTO.getCommonName())) {
							stockOperationItemDTO.setStockItemName(
							    String.format("%1s (%2s)", drugName, stockOperationItemDTO.getCommonName()));
						} else if (drugName != null && conceptName != null) {
							stockOperationItemDTO.setStockItemName(String.format("%1s (%2s)", drugName, conceptName));
						} else if (drugName != null)
							stockOperationItemDTO.setStockItemName(drugName);
						else
							stockOperationItemDTO.setStockItemName(conceptName);
						
					}
				}
			}
		}
		
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
	
	public List<StockBatchDTO> getExpiringStockBatchesDueForNotification(Integer defaultExpiryNotificationNoticePeriod) {
		List<StockBatchDTO> result = getExpiringStockBatchesDueForNotificationWithoutStockItemNoticePeriod(
		    defaultExpiryNotificationNoticePeriod);
		result.addAll(getExpiringStockBatchesDueForNotificationWithStockItemNoticePeriod());
		return result;
		
	}
	
	private List<Integer> getUniqueExpiryNoticePeriods() {
		StringBuilder hqlQuery = new StringBuilder("SELECT DISTINCT si.expiryNotice as expiryNotice\n"
		        + "from stockmanagement.StockItem si where si.expiryNotice is not null and si.voided  = :vdd");
		
		DbSession session = getSession();
		Query query = session.createQuery(hqlQuery.toString());
		query.setParameter("vdd", false);
		List result = query.list();
		List<Integer> integers = new ArrayList<>();
		for (Object object : result) {
			integers.add(((Number) object).intValue());
		}
		return result;
	}
	
	private List<StockBatchDTO> getExpiringStockBatchesDueForNotificationWithStockItemNoticePeriod() {
		List<Integer> noticePeriods = getUniqueExpiryNoticePeriods();
		if (noticePeriods.isEmpty())
			return new ArrayList<>();
		List<StockBatchDTO> result = new ArrayList<>();
		int startIndex = 0;
		int batchSize = 100;
		boolean hasMoreUpdatesToDo = true;
		Date today = DateUtil.today();
		do {
			StringBuilder hqlQuery = new StringBuilder("SELECT sb.id as id,\n" + "sb.uuid as uuid,\n"
			        + "si.uuid as stockItemUuid,\n" + "sb.batchNo as batchNo,\n" + "sb.expiration as expiration,\n"
			        + "sb.expiryNotificationDate as expiryNotificationDate,\n" + "sb.voided as voided\n"
			        + "from stockmanagement.StockBatch sb join\n" + "\t sb.stockItem si ");
			
			StringBuilder hqlFilter = new StringBuilder();
			appendFilter(hqlFilter, "sb.expiration >= :today");
			StringBuilder itemGroupClause = new StringBuilder();
			int paramIndex = 0;
			HashMap<String, Object> parameterList = new HashMap<>();
			List<Integer> batch = noticePeriods.stream().skip(startIndex * batchSize).limit(batchSize)
			        .collect(Collectors.toList());
			for (Integer noticePeriod : batch) {
				appendORFilter(itemGroupClause,
				    String.format("sb.expiration <= :exp%1$s and si.expiryNotice = :pli%1$s", Integer.toString(paramIndex)));
				parameterList.putIfAbsent("pli" + Integer.toString(paramIndex), noticePeriod);
				parameterList.putIfAbsent("exp" + Integer.toString(paramIndex), DateUtils.addDays(today, noticePeriod));
				paramIndex++;
			}
			if (paramIndex == 0) {
				break;
			}
			appendFilter(hqlFilter, itemGroupClause.toString());
			appendFilter(hqlFilter, "sb.expiryNotificationDate is null and sb.voided = :vdd and si.voided  = :vdd");
			parameterList.put("vdd", false);
			parameterList.put("today", DateUtil.today());
			if (hqlFilter.length() > 0) {
				hqlQuery.append(" where ");
				hqlQuery.append(hqlFilter);
			}
			DbSession session = getSession();
			Query query = session.createQuery(hqlQuery.toString());
			for (Map.Entry<String, Object> parameter : parameterList.entrySet()) {
				query.setParameter(parameter.getKey(), parameter.getValue());
			}
			
			query = query.setResultTransformer(new AliasToBeanResultTransformer(StockBatchDTO.class));
			result.addAll(query.list());
			hasMoreUpdatesToDo = batch.size() >= batchSize;
			startIndex++;
		} while (hasMoreUpdatesToDo);
		
		return result;
	}
	
	public Result<StockBatchLineItem> getExpiringStockBatchList(StockExpiryFilter filter) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("select sb.id as stockBatchId, sb.stockItem.id as stockItemId,\n"
		        + "si.drug.drugId as stockItemDrugId,\n" + "si.concept.conceptId as stockItemConceptId,\n"
		        + "si.commonName as commonName,\n" + "si.acronym as acronym,\n"
		        + "si.category.conceptId as stockItemCategoryConceptId,\n" + "si.expiryNotice as expiryNotice,\n"
		        + "sb.dateCreated as dateCreated,\n" + "sb.batchNo as batchNo,\n" + "sb.expiration as expiration\n"
		        + "from stockmanagement.StockBatch sb inner join\n" + " sb.stockItem si\n");
		StringBuilder hqlFilter = new StringBuilder();
		
		if (filter.getStartDate() != null) {
			appendFilter(hqlFilter, "sb.expiration >= :sbexpm");
			parameterList.putIfAbsent("sbexpm", filter.getStartDate());
		}
		
		if (filter.getEndDate() != null) {
			appendFilter(hqlFilter, "sb.expiration <= :sbexpmx");
			parameterList.putIfAbsent("sbexpmx", filter.getEndDate());
		}
		
		appendFilter(hqlFilter, "sb.voided = :vdd");
		parameterList.putIfAbsent("vdd", false);
		
		if (filter.getStockBatchIdMin() != null) {
			appendFilter(hqlFilter, "sb.id > :stockBatchIdMin");
			parameterList.putIfAbsent("stockBatchIdMin", filter.getStockBatchIdMin());
		}
		
		if (filter.getStockItemCategoryConceptId() != null) {
			appendFilter(hqlFilter, "si.category.conceptId = :stockItemCategoryId");
			parameterList.putIfAbsent("stockItemCategoryId", filter.getStockItemCategoryConceptId());
		}
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		Result<StockBatchLineItem> result = new Result<>();
		result.setPageIndex(filter.getStartIndex());
		result.setPageSize(filter.getLimit());
		
		DbSession dbSession = getSession();
		hqlQuery.append(" order by sb.id asc");
		Query query = dbSession.createQuery(hqlQuery.toString());
		if (parameterList != null) {
			for (Map.Entry<String, Object> entry : parameterList.entrySet())
				query.setParameter(entry.getKey(), entry.getValue());
		}
		if (parameterWithList != null) {
			for (Map.Entry<String, Collection> entry : parameterWithList.entrySet())
				query.setParameterList(entry.getKey(), entry.getValue());
		}
		
		query = query.setResultTransformer(new AliasToBeanResultTransformer(StockBatchLineItem.class));
		query.setFirstResult(0);
		query.setMaxResults(filter.getLimit());
		query.setFetchSize(filter.getLimit());
		result.setData(query.list());
		
		if (!result.getData().isEmpty()) {
			List<Integer> conceptIds = new ArrayList<>();
			List<Integer> drugIds = new ArrayList<>();
			
			conceptIds.addAll(result.getData().stream().filter(p -> p.getStockItemCategoryConceptId() != null)
			        .map(p -> p.getStockItemCategoryConceptId()).collect(Collectors.toList()));
			conceptIds.addAll(result.getData().stream().filter(p -> p.getStockItemConceptId() != null)
			        .map(p -> p.getStockItemConceptId()).collect(Collectors.toList()));
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
			
			for (StockBatchLineItem stockBatchLineItem : result.getData()) {
				
				List<ConceptNameDTO> conceptNameDTO = null;
				
				if (stockBatchLineItem.getStockItemCategoryConceptId() != null) {
					conceptNameDTO = conceptNameDTOs.get(stockBatchLineItem.getStockItemCategoryConceptId());
					if (conceptNameDTO != null) {
						stockBatchLineItem.setStockItemCategoryName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (stockBatchLineItem.getStockItemConceptId() != null) {
					conceptNameDTO = conceptNameDTOs.get(stockBatchLineItem.getStockItemConceptId());
					if (conceptNameDTO != null) {
						stockBatchLineItem.setStockItemConceptName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (stockBatchLineItem.getStockItemDrugId() != null) {
					conceptNameDTO = drugNames.get(stockBatchLineItem.getStockItemDrugId());
					if (conceptNameDTO != null) {
						stockBatchLineItem.setStockItemDrugName(conceptNameDTO.get(0).getName());
					}
				}
			}
		}
		return result;
	}
	
	private List<StockBatchDTO> getExpiringStockBatchesDueForNotificationWithoutStockItemNoticePeriod(
	        Integer defaultExpiryNotificationNoticePeriod) {
		StringBuilder hqlQuery = new StringBuilder("SELECT sb.id as id,\n" + "sb.uuid as uuid,\n"
		        + "si.uuid as stockItemUuid,\n" + "sb.batchNo as batchNo,\n" + "sb.expiration as expiration,\n"
		        + "sb.expiryNotificationDate as expiryNotificationDate,\n" + "sb.voided as voided\n"
		        + "from stockmanagement.StockBatch sb join\n" + "\t sb.stockItem si  where sb.expiration >= :today and "
		        + "sb.expiration <= :dfexp and sb.expiryNotificationDate is null and si.expiryNotice is null and sb.voided = :vdd and si.voided  = :vdd");
		
		DbSession session = getSession();
		Query query = session.createQuery(hqlQuery.toString());
		query.setParameter("today", DateUtil.today());
		query.setParameter("dfexp", DateUtils.addDays(DateUtil.today(), defaultExpiryNotificationNoticePeriod));
		query.setParameter("vdd", false);
		query = query.setResultTransformer(new AliasToBeanResultTransformer(StockBatchDTO.class));
		List<StockBatchDTO> result = query.list();
		return result;
	}
	
	public Result<StockBatchDTO> findStockBatches(StockBatchSearchFilter filter) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder(
		        "SELECT sb.id as id,\n" + "sb.uuid as uuid,\n" + "si.uuid as stockItemUuid,\n" + "sb.batchNo as batchNo,\n"
		                + "sb.expiration as expiration,\n" + "sb.expiryNotificationDate as expiryNotificationDate,\n"
		                + "sb.voided as voided\n" + "from stockmanagement.StockBatch sb join\n" + "\t sb.stockItem si\n");
		
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
		
		Result<StockBatchDTO> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		
		result.setData(
		    executeQuery(StockBatchDTO.class, hqlQuery, result, " order by sb.id", parameterList, parameterWithList));
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
	
	public StockOperationLink getStockOperationLinkByUuid(String uuid) {
		return (StockOperationLink) getSession().createCriteria(StockOperationLink.class).add(Restrictions.eq("uuid", uuid))
		        .uniqueResult();
	}
	
	public StockOperationLink saveStockOperationLink(StockOperationLink stockOperationLink) {
		getSession().saveOrUpdate(stockOperationLink);
		return stockOperationLink;
	}
	
	public void voidStockOperationItem(String stockOperationItemId, String reason, int voidedBy) {
		DbSession session = getSession();
		Query query = session.createQuery(
		    "UPDATE stockmanagement.StockOperationItem SET voided=1, dateVoided=:dateVoided, voidedBy=:voidedBy, voidReason=:reason WHERE uuid in (:uuid)");
		query.setParameter("uuid", stockOperationItemId);
		query.setDate("dateVoided", new Date());
		query.setInteger("voidedBy", voidedBy);
		query.setString("reason", reason);
		query.executeUpdate();
	}
	
	public long getStockOperationItemCount(Integer stockOperationId) {
		Criteria criteria = getSession().createCriteria(StockOperationItem.class)
		        .add(Restrictions.eq("stockOperation.id", stockOperationId)).add(Restrictions.eq("voided", false));
		
		criteria.setProjection(Projections.rowCount());
		return ((Number) criteria.uniqueResult()).longValue();
	}
	
	public Result<StockItemDTO> getStockItemConceptRefs(List<Integer> stockItemIds) {
		if (stockItemIds == null || stockItemIds.isEmpty())
			return new Result<>(new ArrayList<>(), 0);
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder(
		        "select si.id as id,\n" + "si.drug.drugId as drugId,\n" + "d.uuid as drugUuid,\n"
		                + "c.conceptId as conceptId,\n" + "c.uuid as conceptUuid,\n" + "d.strength as drugStrength\n"
		                + "from stockmanagement.StockItem si left join\n" + " si.drug d left join\n" + "\t si.concept c\n");
		
		StringBuilder hqlFilter = new StringBuilder();
		appendFilter(hqlFilter, "si.id in (:ids)");
		parameterWithList.putIfAbsent("ids", stockItemIds);
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		Result<StockItemDTO> result = new Result<>();
		result.setData(
		    executeQuery(StockItemDTO.class, hqlQuery, result, " order by si.id asc", new HashMap<>(), parameterWithList));
		
		return result;
	}
	
	public Result<StockOperationLinkDTO> findStockOperationLinks(String stockOperationUuid) {
		return findStockOperationLinks(stockOperationUuid, null);
	}
	
	public Result<StockOperationLinkDTO> findStockOperationLinks(String parentOrChildStockOperationUuid,
	        String childStockOperationUuid) {
		if (StringUtils.isBlank(parentOrChildStockOperationUuid) && StringUtils.isBlank(childStockOperationUuid))
			return new Result<>(new ArrayList<>(), 0);
		
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("SELECT sol.id as id,\n" + "sol.uuid as uuid,\n"
		        + "p.uuid as parentUuid,\n" + "p.operationNumber as parentOperationNumber,\n"
		        + "psot.name as parentOperationTypeName,\n" + "p.status as parentStatus,\n" + "p.voided as parentVoided,\n"
		        + "c.uuid as childUuid,\n" + "c.operationNumber as childOperationNumber,\n"
		        + "csot.name as childOperationTypeName,\n" + "c.status as childStatus,\n" + "c.voided as childVoided\n"
		        + "from stockmanagement.StockOperationLink sol join\n"
		        + "\t sol.parent p join p.stockOperationType psot join\n" + " sol.child c join c.stockOperationType csot\n");
		
		StringBuilder hqlFilter = new StringBuilder();
		if (!StringUtils.isBlank(parentOrChildStockOperationUuid)) {
			appendFilter(hqlFilter, "p.uuid = :suuid or c.uuid = :suuid");
			parameterList.putIfAbsent("suuid", parentOrChildStockOperationUuid);
		}
		if (!StringUtils.isBlank(childStockOperationUuid)) {
			appendFilter(hqlFilter, "c.uuid = :suuid");
			parameterList.putIfAbsent("suuid", childStockOperationUuid);
		}
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		Result<StockOperationLinkDTO> result = new Result<>();
		result.setPageIndex(0);
		result.setPageSize(10);
		result.setData(executeQuery(StockOperationLinkDTO.class, hqlQuery, result, " order by sol.id", parameterList,
		    parameterWithList));
		return result;
	}
	
	public List<Drug> getDrugs(Collection<Integer> drugIds) {
		if (drugIds == null || drugIds.isEmpty())
			return new ArrayList<>();
		DbSession dbSession = getSession();
		Criteria criteria = dbSession.createCriteria(Drug.class, "d");
		criteria.add(Restrictions.in("d.drugId", drugIds));
		return criteria.list();
	}
	
	public List<Concept> getConcepts(Collection<Integer> conceptIds) {
		if (conceptIds == null || conceptIds.isEmpty())
			return new ArrayList<>();
		DbSession dbSession = getSession();
		Criteria criteria = dbSession.createCriteria(Concept.class, "c");
		criteria.add(Restrictions.in("c.conceptId", conceptIds));
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
	
	public List<StockItemPackagingUOM> getStockItemPackagingUOMs(
	        List<StockItemPackagingUOMSearchFilter.ItemGroupFilter> filters) {
		if (filters == null || filters.isEmpty())
			return new ArrayList<>();
		DbSession dbSession = getSession();
		Criteria criteria = dbSession.createCriteria(StockItemPackagingUOM.class, "s");
		if (filters.size() == 1) {
			StockItemPackagingUOMSearchFilter.ItemGroupFilter filter = filters.get(0);
			criteria.add(Restrictions.and(Restrictions.eq("s.stockItem.id", filter.getStockItemId()),
			    Restrictions.in("s.packagingUom.conceptId", filter.getPackagingUomIds())));
		} else {
			StockItemPackagingUOMSearchFilter.ItemGroupFilter filter0 = filters.get(0);
			StockItemPackagingUOMSearchFilter.ItemGroupFilter filter1 = filters.get(1);
			LogicalExpression orFilter = Restrictions.or(
			    Restrictions.and(Restrictions.eq("s.stockItem.id", filter0.getStockItemId()),
			        Restrictions.in("s.packagingUom.conceptId", filter0.getPackagingUomIds())),
			    Restrictions.and(Restrictions.eq("s.stockItem.id", filter1.getStockItemId()),
			        Restrictions.in("s.packagingUom.conceptId", filter1.getPackagingUomIds())));
			if (filters.size() > 2) {
				for (int i = 2; i < filters.size(); i++) {
					StockItemPackagingUOMSearchFilter.ItemGroupFilter filter = filters.get(i);
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
	
	public Result<StockRuleDTO> findStockRules(StockRuleSearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("select sr.uuid as uuid, sr.id as id, sr.stockItem.id as stockItemId,\n"
		        + "si.uuid as stockItemUuid," + "sr.name as name,\n" + "sr.description as description,\n"
		        + "sr.location.locationId  as locationId,\n" + "l.uuid  as locationUuid,\n" + "l.name as locationName,\n"
		        + "sr.quantity as quantity,\n" + "sipu.id as stockItemPackagingUOMId,\n"
		        + "sipu.uuid as stockItemPackagingUOMUuid,\n" + "pu.conceptId as packagingUoMId,\n"
		        + "sr.enabled as enabled, sr.evaluationFrequency as evaluationFrequency,\n"
		        + "sr.lastEvaluation as lastEvaluation, sr.nextEvaluation as nextEvaluation,\n"
		        + "sr.actionFrequency as actionFrequency, sr.lastActionDate as lastActionDate,\n"
		        + "sr.nextActionDate as nextActionDate,\n" + "sr.alertRole as alertRole, sr.mailRole as mailRole,\n"
		        + "sr.creator.userId as creator,\n" + "sr.dateCreated as dateCreated,\n"
		        + "sr.enableDescendants as enableDescendants,\n" + "sr.voided as voided\n"
		        + "from stockmanagement.StockRule sr join sr.stockItem si join\n" + " sr.location l join \n"
		        + "\t sr.stockItemPackagingUOM sipu left join sipu.packagingUom pu");
		
		StringBuilder hqlFilter = new StringBuilder();
		
		if (filter.getId() != null) {
			appendFilter(hqlFilter, "sr.id = :id");
			parameterList.put("id", filter.getId());
		}
		
		if (filter.getUuids() != null && !filter.getUuids().isEmpty()) {
			appendFilter(hqlFilter, "sr.uuid in (:uuid)");
			parameterWithList.put("uuid", filter.getUuids());
		}
		List<Integer> locationIdsToFilter = null;
		if (filter.getLocationUuids() != null && !filter.getLocationUuids().isEmpty()) {
			Map<String, Integer> locationIds = getLocationIds(filter.getLocationUuids());
			if (locationIds.isEmpty()) {
				return new Result<>(new ArrayList<>(), 0);
			}
			locationIdsToFilter = locationIds.values().stream().collect(Collectors.toList());
		}
		
		if (recordPrivilegeFilters != null) {
			if (locationIdsToFilter != null) {
				locationIdsToFilter
				        .removeIf(p -> !recordPrivilegeFilters.stream().anyMatch(x -> x.getLocationId().equals(p)));
				if (locationIdsToFilter.isEmpty()) {
					return new Result<>(new ArrayList<>(), 0);
				}
			} else {
				locationIdsToFilter = recordPrivilegeFilters.stream().map(p -> p.getLocationId()).distinct()
				        .collect(Collectors.toList());
			}
		}
		
		if (filter.getStockItemUuids() != null && !filter.getStockItemUuids().isEmpty()) {
			Map<String, Integer> stockItemIds = getStockItemIds(filter.getStockItemUuids());
			if (stockItemIds.isEmpty()) {
				return new Result<>(new ArrayList<>(), 0);
			}
			appendFilter(hqlFilter, "sr.stockItem.id in (:siids)");
			parameterWithList.put("siids", stockItemIds.values());
		}
		
		if (locationIdsToFilter != null && !locationIdsToFilter.isEmpty()) {
			appendFilter(hqlFilter, "sr.location.locationId in (:liids)");
			parameterWithList.put("liids", locationIdsToFilter);
		}
		
		if (filter.getLastEvaluationMin() != null) {
			appendFilter(hqlFilter, "sr.lastEvaluation >= :lem or sr.lastEvaluation is null");
			parameterList.putIfAbsent("lem", filter.getLastEvaluationMin());
		}
		
		if (filter.getLastEvaluationMax() != null) {
			appendFilter(hqlFilter, "sr.lastEvaluation <= :lemx or sr.lastEvaluation is null");
			parameterList.putIfAbsent("lemx", filter.getLastEvaluationMax());
		}
		
		if (filter.getNextEvaluationMin() != null) {
			appendFilter(hqlFilter, "sr.nextEvaluation >= :nem or sr.nextEvaluation is null");
			parameterList.putIfAbsent("nem", filter.getNextEvaluationMin());
		}
		
		if (filter.getNextEvaluationMax() != null) {
			appendFilter(hqlFilter, "sr.nextEvaluation <= :nemx or sr.nextEvaluation is null");
			parameterList.putIfAbsent("nemx", filter.getNextEvaluationMax());
		}
		
		if (filter.getLastActionDateMin() != null) {
			appendFilter(hqlFilter, "sr.lastActionDate >= :laem or sr.lastActionDate is null");
			parameterList.putIfAbsent("laem", filter.getLastActionDateMin());
		}
		
		if (filter.getLastActionDateMax() != null) {
			appendFilter(hqlFilter, "sr.lastActionDate <= :laemx or sr.lastActionDate is null");
			parameterList.putIfAbsent("laemx", filter.getLastActionDateMax());
		}
		
		if (filter.getHasNotificationRoleSet() != null) {
			if (filter.getHasNotificationRoleSet()) {
				appendFilter(hqlFilter, "sr.alertRole is not null or sr.mailRole is not null");
			} else {
				appendFilter(hqlFilter, "sr.alertRole is null and sr.mailRole is null");
			}
		}
		
		if (filter.getEnabled() != null) {
			appendFilter(hqlFilter, "sr.enabled = :enabled");
			parameterList.putIfAbsent("enabled", filter.getEnabled());
		}
		
		if (!filter.getIncludeVoided()) {
			appendFilter(hqlFilter, "sr.voided = :vdd");
			parameterList.putIfAbsent("vdd", false);
		}
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		Result<StockRuleDTO> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		
		result.setData(
		    executeQuery(StockRuleDTO.class, hqlQuery, result, " order by sr.id desc", parameterList, parameterWithList));
		
		if (!result.getData().isEmpty()) {
			List<Integer> conceptNamesToFetch = result.getData().stream().map(p -> p.getPackagingUoMId())
			        .filter(p -> p != null).distinct().collect(Collectors.toList());
			Map<Integer, List<ConceptNameDTO>> conceptNameDTOs = conceptNamesToFetch.isEmpty() ? null
			        : getConceptNamesByConceptIds(conceptNamesToFetch).stream()
			                .collect(Collectors.groupingBy(ConceptNameDTO::getConceptId));
			
			for (StockRuleDTO stockRuleDTO : result.getData()) {
				if (conceptNameDTOs != null) {
					if (stockRuleDTO.getPackagingUoMId() != null
					        && conceptNameDTOs.containsKey(stockRuleDTO.getPackagingUoMId())) {
						stockRuleDTO
						        .setPackagingUomName(conceptNameDTOs.get(stockRuleDTO.getPackagingUoMId()).get(0).getName());
					}
				}
			}
		}
		
		return result;
	}
	
	public void voidStockRules(List<String> stockRuleUuids, String reason, int voidedBy) {
		DbSession session = getSession();
		Query query = session.createQuery(
		    "UPDATE stockmanagement.StockRule SET voided=1, dateVoided=:dateVoided, voidedBy=:voidedBy, voidReason=:reason WHERE uuid in (:uuidList)");
		query.setParameterList("uuidList", stockRuleUuids);
		query.setDate("dateVoided", new Date());
		query.setInteger("voidedBy", voidedBy);
		query.setString("reason", reason);
		query.executeUpdate();
	}
	
	public List<StockRuleNotificationUser> getDueStockRules(Integer lastStockRuleId, int limit) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("select sr.id as id, sr.stockItem.id as stockItemId,\n"
		        + "sr.location.locationId as locationId,\n"
		        + "sr.quantity * sipu.factor as quantity, sipu.factor as factor,\n"
		        + "sipu.packagingUom.conceptId as packagingConceptId,\n" + "sr.enableDescendants as enableDescendants,\n"
		        + "sr.evaluationFrequency as evaluationFrequency,\n" + "sr.actionFrequency as actionFrequency,\n"
		        + "sr.alertRole as alertRole, sr.mailRole as mailRole\n"
		        + "from stockmanagement.StockRule sr join sr.stockItem si join\n" + "\t sr.stockItemPackagingUOM sipu");
		
		parameterList.putIfAbsent("truthy", true);
		parameterList.putIfAbsent("falsy", false);
		parameterList.putIfAbsent("today", new Date());
		parameterList.putIfAbsent("idmin", lastStockRuleId == null ? 0 : lastStockRuleId);
		StringBuilder hqlFilter = new StringBuilder();
		appendFilter(hqlFilter, "sr.id > :idmin");
		appendFilter(hqlFilter, "sr.nextEvaluation is null or sr.nextEvaluation <= :today");
		appendFilter(hqlFilter, "sr.nextActionDate is null or sr.nextActionDate <= :today");
		appendFilter(hqlFilter, "sr.enabled = :truthy");
		appendFilter(hqlFilter, "sr.voided = :falsy");
		appendFilter(hqlFilter, "si.voided = :falsy");
		hqlQuery.append(" where ");
		hqlQuery.append(hqlFilter);
		hqlQuery.append(" order by sr.id");
		
		DbSession dbSession = getSession();
		Query query = dbSession.createQuery(hqlQuery.toString());
		for (Map.Entry<String, Object> entry : parameterList.entrySet())
			query.setParameter(entry.getKey(), entry.getValue());
		query = query.setResultTransformer(new AliasToBeanResultTransformer(StockRuleNotificationUser.class));
		query.setMaxResults(limit);
		query.setFetchSize(limit);
		return query.list();
	}
	
	public void updateStockBatchExpiryNotificationDate(Collection<Integer> stockBatchIds, Date notificationDate) {
		if (stockBatchIds == null || stockBatchIds.isEmpty())
			return;
		int startIndex = 0;
		boolean hasMoreUpdatesToDo = true;
		do {
			DbSession session = getSession();
			Query query = session.createQuery(
			    "UPDATE stockmanagement.StockBatch SET expiryNotificationDate = :nfdate WHERE id in (:stockBatchIds)");
			List<Integer> batch = stockBatchIds.stream().skip(startIndex * 100).limit(100).collect(Collectors.toList());
			if (batch.isEmpty()) {
				break;
			}
			query.setParameterList("stockBatchIds", batch);
			query.setParameter("nfdate", notificationDate);
			query.executeUpdate();
			hasMoreUpdatesToDo = batch.size() >= 100;
			startIndex++;
		} while (hasMoreUpdatesToDo);
	}
	
	public void updateStockRuleJobNextEvaluationDate(List<Integer> stockRuleIds, Date nextEvaluationDate) {
		if (stockRuleIds == null || stockRuleIds.isEmpty())
			return;
		int startIndex = 0;
		boolean hasMoreUpdatesToDo = true;
		do {
			DbSession session = getSession();
			Query query = session.createQuery(
			    "UPDATE stockmanagement.StockRule SET lastEvaluation = :today, nextEvaluation = :nextdate WHERE id in (:stockRuleIds)");
			List<Integer> batch = stockRuleIds.stream().skip(startIndex * 100).limit(100).collect(Collectors.toList());
			if (batch.isEmpty()) {
				break;
			}
			query.setParameterList("stockRuleIds", batch);
			query.setParameter("nextdate", nextEvaluationDate);
			query.setParameter("today", new Date());
			query.executeUpdate();
			hasMoreUpdatesToDo = batch.size() >= 100;
			startIndex++;
		} while (hasMoreUpdatesToDo);
	}
	
	public void updateStockRuleJobNextActionDate(List<Integer> stockRuleIds, Date nextEvaluationDate) {
		if (stockRuleIds == null || stockRuleIds.isEmpty())
			return;
		int startIndex = 0;
		boolean hasMoreUpdatesToDo = true;
		do {
			DbSession session = getSession();
			Query query = session.createQuery(
			    "UPDATE stockmanagement.StockRule SET lastActionDate = :today, nextActionDate = :nextdate WHERE id in (:stockRuleIds)");
			List<Integer> batch = stockRuleIds.stream().skip(startIndex * 100).limit(100).collect(Collectors.toList());
			if (batch.isEmpty()) {
				break;
			}
			query.setParameterList("stockRuleIds", batch);
			query.setParameter("nextdate", nextEvaluationDate);
			query.setParameter("today", new Date());
			query.executeUpdate();
			hasMoreUpdatesToDo = batch.size() >= 100;
			startIndex++;
		} while (hasMoreUpdatesToDo);
	}
	
	public Map<Integer, String> getStockItemNames(List<Integer> stockItemIds) {
		if (stockItemIds == null || stockItemIds.isEmpty())
			return new HashMap<>();
		Map<Integer, String> stockItemNames = null;
		int startIndex = 0;
		boolean hasMoreUpdatesToDo = true;
		do {
			List<Integer> batch = stockItemIds.stream().skip(startIndex * 100).limit(100).collect(Collectors.toList());
			if (batch.isEmpty()) {
				break;
			}
			Map<Integer, String> result = getStockItemNamesInternal(batch);
			if (stockItemNames == null) {
				stockItemNames = result;
			} else {
				stockItemNames.putAll(result);
			}
			hasMoreUpdatesToDo = batch.size() >= 100;
			startIndex++;
		} while (hasMoreUpdatesToDo);
		return stockItemNames;
	}
	
	private Map<Integer, String> getStockItemNamesInternal(List<Integer> stockItemIds) {
		if (stockItemIds == null || stockItemIds.isEmpty())
			return new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("select si.id as id, d.name as drugName, c.conceptId as conceptId\n"
		        + "from stockmanagement.StockItem si left join\n" + " si.drug d left join\n"
		        + "\t si.concept c  where si.id in (:ids)");
		
		Query query = getSession().createQuery(hqlQuery.toString());
		query.setParameterList("ids", stockItemIds);
		
		List<Integer> result = new ArrayList<>();
		List partialResult = query.list();
		if (partialResult.isEmpty())
			return new HashMap<>();
		
		List<Integer> conceptNamesToFetch = new ArrayList<>();
		for (Object object : partialResult) {
			if (((Object[]) object)[2] != null) {
				Integer value = Integer.valueOf(((Number) (((Object[]) object)[2])).intValue());
				conceptNamesToFetch.add(value);
			}
		}
		Map<Integer, List<ConceptNameDTO>> conceptNameDTOs = null;
		if (!conceptNamesToFetch.isEmpty()) {
			List<Integer> distinctConceptIds = conceptNamesToFetch.stream().distinct().collect(Collectors.toList());
			conceptNameDTOs = getConceptNamesByConceptIds(distinctConceptIds).stream()
			        .collect(Collectors.groupingBy(ConceptNameDTO::getConceptId));
		}
		Map<Integer, String> stockItemNames = new HashMap<>();
		for (Object object : partialResult) {
			if (((Object[]) object)[2] != null) {
				Integer value = Integer.valueOf(((Number) (((Object[]) object)[2])).intValue());
				List<ConceptNameDTO> conceptNameDTOList = conceptNameDTOs.getOrDefault(value, null);
				if (conceptNameDTOList != null && conceptNameDTOList.isEmpty()) {
					stockItemNames.putIfAbsent(Integer.valueOf(((Number) (((Object[]) object)[0])).intValue()),
					    String.format("%1s %2s", ((Object[]) object)[1], conceptNameDTOList.get(0).getName()));
				} else {
					stockItemNames.putIfAbsent(Integer.valueOf(((Number) (((Object[]) object)[0])).intValue()),
					    (String) ((Object[]) object)[1]);
				}
			} else {
				stockItemNames.putIfAbsent(Integer.valueOf(((Number) (((Object[]) object)[0])).intValue()),
				    (String) ((Object[]) object)[1]);
			}
		}
		return stockItemNames;
	}
	
	public BatchJobOwner getBatchJobOwnerByUuid(String uuid) {
		return (BatchJobOwner) getSession().createCriteria(BatchJobOwner.class).add(Restrictions.eq("uuid", uuid))
		        .uniqueResult();
	}
	
	public BatchJobOwner saveBatchJobOwner(BatchJobOwner batchJobOwner) {
		getSession().saveOrUpdate(batchJobOwner);
		return batchJobOwner;
	}
	
	public BatchJob getBatchJobById(Integer id) {
		return (BatchJob) getSession().createCriteria(BatchJob.class).add(Restrictions.eq("id", id)).uniqueResult();
	}
	
	public BatchJob getBatchJobByUuid(String uuid) {
		return (BatchJob) getSession().createCriteria(BatchJob.class).add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	public BatchJob saveBatchJob(BatchJob batchJob) {
		getSession().saveOrUpdate(batchJob);
		return batchJob;
	}
	
	public BatchJob getNextActiveBatchJob() {
		DbSession dbSession = getSession();
		Criteria criteria = dbSession.createCriteria(BatchJob.class, "bj");
		criteria.add(Restrictions.in("bj.status", Arrays.asList(BatchJobStatus.Pending, BatchJobStatus.Running)));
		criteria.add(Restrictions.eq("bj.voided", false));
		
		Result<BatchJob> result = new Result<>();
		result.setPageIndex(0);
		result.setPageSize(1);
		
		result.setData(executeCriteria(criteria, result, Order.asc("bj.id")));
		return result.getData().isEmpty() ? null : result.getData().get(0);
	}
	
	public Result<BatchJobDTO> findBatchJobs(BatchJobSearchFilter filter,
	        HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("select bj.uuid as uuid, bj.id as id, bj.batchJobType as batchJobType,\n"
		        + "bj.status as status," + "bj.description as description,\n" + "bj.startTime as startTime,\n"
		        + "bj.endTime  as endTime,\n" + "bj.expiration as expiration,\n" + "bj.parameters as parameters,\n"
		        + "bj.privilegeScope as privilegeScope,\n" + "bj.locationScope.locationId as locationScopeId,\n"
		        + "ls.uuid as locationScopeUuid,\n" + "ls.name as locationScope,\n"
		        + "bj.executionState as executionState,\n" + "bj.cancelReason as cancelReason,\n"
		        + "bj.cancelledDate as cancelledDate,\n" + "bj.exitMessage as exitMessage,\n"
		        + "bj.completedDate as completedDate,\n" + "bj.dateCreated as dateCreated,\n"
		        + "bj.creator.userId as creator,\n" + "bj.voided as voided,\n"
		        + "bj.outputArtifactSize as outputArtifactSize,\n" + "bj.outputArtifactFileExt as outputArtifactFileExt,\n"
		        + "bj.outputArtifactViewable as outputArtifactViewable,\n" + "bj.cancelledBy.userId as cancelledBy\n"
		        + " from stockmanagement.BatchJob bj left join bj.locationScope ls\n");
		
		StringBuilder hqlFilter = new StringBuilder();
		
		if (filter.getBatchJobIds() != null && !filter.getBatchJobIds().isEmpty()) {
			appendFilter(hqlFilter, "bj.id in (:id)");
			parameterWithList.put("id", filter.getBatchJobIds());
		}
		
		if (filter.getBatchJobUuids() != null && !filter.getBatchJobUuids().isEmpty()) {
			appendFilter(hqlFilter, "bj.uuid in (:uuid)");
			parameterWithList.put("uuid", filter.getBatchJobUuids());
		}
		
		if (filter.getBatchJobType() != null) {
			appendFilter(hqlFilter, "bj.batchJobType = :batchJobType");
			parameterList.put("batchJobType", filter.getBatchJobType());
		}
		
		if (filter.getBatchJobStatus() != null && !filter.getBatchJobStatus().isEmpty()) {
			appendFilter(hqlFilter, "bj.status in (:status)");
			parameterWithList.put("status", filter.getBatchJobStatus());
		}
		
		if (filter.getDateCreatedMin() != null) {
			appendFilter(hqlFilter, "bj.dateCreated >= :ldc");
			parameterList.putIfAbsent("ldc", filter.getDateCreatedMin());
		}
		
		if (filter.getDateCreatedMax() != null) {
			appendFilter(hqlFilter, "bj.dateCreated <= :ldcx");
			parameterList.putIfAbsent("ldcx", filter.getDateCreatedMax());
		}
		
		if (filter.getCompletedDateMin() != null) {
			appendFilter(hqlFilter, "bj.completedDate >= :lcd");
			parameterList.putIfAbsent("lcd", filter.getCompletedDateMin());
		}
		
		if (filter.getCompletedDateMax() != null) {
			appendFilter(hqlFilter, "bj.completedDate <= :lcdx");
			parameterList.putIfAbsent("lcdx", filter.getCompletedDateMax());
		}
		
		List<Integer> locationIdsToFilter = null;
		if (filter.getLocationScopeIds() != null && !filter.getLocationScopeIds().isEmpty()) {
			locationIdsToFilter = filter.getLocationScopeIds();
		}
		
		if (recordPrivilegeFilters != null) {
			if (locationIdsToFilter != null) {
				locationIdsToFilter
				        .removeIf(p -> !recordPrivilegeFilters.stream().anyMatch(x -> x.getLocationId().equals(p)));
				if (locationIdsToFilter.isEmpty()) {
					return new Result<>(new ArrayList<>(), 0);
				}
			} else {
				locationIdsToFilter = recordPrivilegeFilters.stream().map(p -> p.getLocationId()).distinct()
				        .collect(Collectors.toList());
			}
		}
		
		if (locationIdsToFilter != null && !locationIdsToFilter.isEmpty()) {
			appendFilter(hqlFilter, "bj.locationScope.locationId in (:liids)");
			parameterWithList.put("liids", locationIdsToFilter);
		}
		
		if (filter.getPrivilegeScope() != null) {
			appendFilter(hqlFilter, "bj.privilegeScope = :pvgsp");
			parameterList.put("pvgsp", filter.getPrivilegeScope());
		}
		
		if (filter.getParameters() != null) {
			appendFilter(hqlFilter, "bj.parameters = :parameters");
			parameterList.put("parameters", filter.getParameters());
		}
		
		if (!filter.getIncludeVoided()) {
			appendFilter(hqlFilter, "bj.voided = :vdd");
			parameterList.putIfAbsent("vdd", false);
		}
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		Result<BatchJobDTO> result = new Result<>();
		if (filter.getLimit() != null) {
			result.setPageIndex(filter.getStartIndex());
			result.setPageSize(filter.getLimit());
		}
		
		result.setData(
		    executeQuery(BatchJobDTO.class, hqlQuery, result, " order by bj.id desc", parameterList, parameterWithList));
		
		if (!result.getData().isEmpty()) {
			Result<BatchJobOwnerDTO> batchJobOwners = findBatchJobOwnersInternal(
			    result.getData().stream().map(p -> p.getId()).collect(Collectors.toList()), false);
			List<Integer> userIds = result.getData().stream().map(p -> p.getCreator()).filter(p -> p != null).distinct()
			        .collect(Collectors.toList());
			userIds.addAll(result.getData().stream().map(p -> p.getCancelledBy()).filter(p -> p != null).distinct()
			        .collect(Collectors.toList()));
			userIds.addAll(batchJobOwners.getData().stream().map(p -> p.getOwnerUserId()).filter(p -> p != null).distinct()
			        .collect(Collectors.toList()));
			List<UserPersonNameDTO> personNames = getPersonNameByUserIds(
			    userIds.stream().distinct().collect(Collectors.toList()));
			for (BatchJobOwnerDTO batchJobOwnerDTO : batchJobOwners.getData()) {
				if (batchJobOwnerDTO.getOwnerUserId() != null) {
					Optional<UserPersonNameDTO> userPersonNameDTO = personNames.stream()
					        .filter(p -> p.getUserId().equals(batchJobOwnerDTO.getOwnerUserId())).findFirst();
					if (userPersonNameDTO.isPresent()) {
						batchJobOwnerDTO.setOwnerFamilyName(userPersonNameDTO.get().getFamilyName());
						batchJobOwnerDTO.setOwnerGivenName(userPersonNameDTO.get().getGivenName());
						batchJobOwnerDTO.setOwnerUserUuid(userPersonNameDTO.get().getUuid());
					}
				}
			}
			for (BatchJobDTO batchJobDTO : result.getData()) {
				if (batchJobDTO.getCreator() != null) {
					Optional<UserPersonNameDTO> userPersonNameDTO = personNames.stream()
					        .filter(p -> p.getUserId().equals(batchJobDTO.getCreator())).findFirst();
					if (userPersonNameDTO.isPresent()) {
						batchJobDTO.setCreatorFamilyName(userPersonNameDTO.get().getFamilyName());
						batchJobDTO.setCreatorGivenName(userPersonNameDTO.get().getGivenName());
						batchJobDTO.setCreatorUuid(userPersonNameDTO.get().getUuid());
					}
				}
				
				if (batchJobDTO.getCancelledBy() != null) {
					Optional<UserPersonNameDTO> userPersonNameDTO = personNames.stream()
					        .filter(p -> p.getUserId().equals(batchJobDTO.getCancelledBy())).findFirst();
					if (userPersonNameDTO.isPresent()) {
						batchJobDTO.setCancelledByFamilyName(userPersonNameDTO.get().getFamilyName());
						batchJobDTO.setCancelledByGivenName(userPersonNameDTO.get().getGivenName());
						batchJobDTO.setCancelledByUuid(userPersonNameDTO.get().getUuid());
					}
				}
				
				batchJobDTO.setOwners(batchJobOwners.getData().stream()
				        .filter(p -> p.getBatchJobId().equals(batchJobDTO.getId())).collect(Collectors.toList()));
				for (BatchJobOwnerDTO batchJobOwnerDTO : batchJobDTO.getOwners()) {
					batchJobOwnerDTO.setBatchJobUuid(batchJobDTO.getUuid());
				}
			}
		}
		
		return result;
	}
	
	public Result<BatchJobOwnerDTO> findBatchJobOwners(List<Integer> batchJobIds) {
		return findBatchJobOwnersInternal(batchJobIds, true);
	}
	
	private Result<BatchJobOwnerDTO> findBatchJobOwnersInternal(List<Integer> batchJobIds, boolean setNames) {
		if (batchJobIds == null || batchJobIds.isEmpty()) {
			return new Result<>(new ArrayList<>(), 0);
		}
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("select bjo.uuid as uuid, bjo.id as id, bjo.batchJob.id as batchJobId,\n"
		        + "bjo.owner.userId as ownerUserId,\n" + "bjo.dateCreated as dateCreated\n"
		        + "from stockmanagement.BatchJobOwner bjo\n");
		
		StringBuilder hqlFilter = new StringBuilder();
		appendFilter(hqlFilter, "bjo.batchJob.id in (:batchJobId)");
		parameterWithList.put("batchJobId", batchJobIds);
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		Result<BatchJobOwnerDTO> result = new Result<>();
		result.setData(executeQuery(BatchJobOwnerDTO.class, hqlQuery, result, " order by bjo.id asc", parameterList,
		    parameterWithList));
		
		if (setNames) {
			List<Integer> userIds = result.getData().stream().map(p -> p.getOwnerUserId()).filter(p -> p != null).distinct()
			        .collect(Collectors.toList());
			List<UserPersonNameDTO> personNames = getPersonNameByUserIds(
			    userIds.stream().distinct().collect(Collectors.toList()));
			for (BatchJobOwnerDTO batchJobOwnerDTO : result.getData()) {
				if (batchJobOwnerDTO.getOwnerUserId() != null) {
					Optional<UserPersonNameDTO> userPersonNameDTO = personNames.stream()
					        .filter(p -> p.getUserId().equals(batchJobOwnerDTO.getOwnerUserId())).findFirst();
					if (userPersonNameDTO.isPresent()) {
						batchJobOwnerDTO.setOwnerFamilyName(userPersonNameDTO.get().getFamilyName());
						batchJobOwnerDTO.setOwnerGivenName(userPersonNameDTO.get().getGivenName());
						batchJobOwnerDTO.setOwnerUserUuid(userPersonNameDTO.get().getUuid());
					}
				}
			}
		}
		
		return result;
	}
	
	public Result<StockOperationLineItem> findStockOperationLineItems(StockOperationLineItemFilter filter) {
		HashMap<String, Object> parameterList = new HashMap<>();
		HashMap<String, Collection> parameterWithList = new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("select so.id as stockOperationId,\n" + "sot.name as operationTypeName,\n"
		        + "so.operationDate as operationDate,\n" + "so.operationNumber as operationNumber,\n"
		        + "so.completedBy.userId as completedBy,\n" + "so.completedDate as completedDate,\n"
		        + "coalesce(sorcel.name,sorces.name) as sourceName,\n"
		        + "coalesce(destl.name,dests.name) as destinationName,\n" + "so.reason.conceptId as reasonId,\n"
		        + "so.responsiblePerson.userId as responsiblePerson,\n"
		        + "so.responsiblePersonOther as responsiblePersonOther,\n" + "so.remarks as remarks,\n"
		        + "so.status as stockOperationStatus,\n" + "so.creator.userId as creator,\n"
		        + "so.dateCreated as dateCreated,\n" + "soi.id as stockOperationItemId,\n"
		        + "soi.stockItem.id as stockItemId,\n" + "si.drug.drugId as stockItemDrugId,\n"
		        + "si.concept.conceptId as stockItemConceptId,\n" + "si.commonName as commonName,\n"
		        + "si.acronym as acronym,\n" + "si.category.conceptId as stockItemCategoryConceptId,\n"
		        + "sb.batchNo as batchNo,\n" + "sb.expiration as expiration,\n" + "soi.quantity as quantity,\n"
		        + "soi.purchasePrice as purchasePrice,\n" + "sipu.packagingUom.conceptId as packagingUoMId,\n"
		        + "sipu.factor as stockItemPackagingUOMFactor,\n"
		        + (filter.includeRequisitionInfo() ? "sop.operationNumber as requisitionOperationNumber," : "")
		        + "qrqpu.factor as quantityRequestedPackagingUOMFactor,\n" + "soi.quantityRequested as quantityRequested,\n"
		        + "qrqpu.packagingUom.conceptId as quantityRequestedPackagingUoMId\n"
		        + "from stockmanagement.StockOperation so inner join\n" + " so.stockOperationType sot join \n"
		        + " so.stockOperationItems soi join\n" + " soi.stockItem si left join\n" + " soi.stockBatch sb left join\n"
		        + " soi.stockItemPackagingUOM sipu left join soi.quantityRequestedPackagingUOM qrqpu left join \n"
		        + " so.destination dest left join dest.location destl left join dest.stockSource dests left join\n"
		        + " so.source sorce left join sorce.location sorcel left join sorce.stockSource sorces\n"
		        + (filter.includeRequisitionInfo() ? " left join so.parentStockOperationLinks sol left join sol.parent sop"
		                : ""));
		StringBuilder hqlFilter = new StringBuilder();
		
		if (filter.getStockOperationIdMin() != null) {
			appendFilter(hqlFilter, "so.id >= :stockOperationId");
			parameterList.putIfAbsent("stockOperationId", filter.getStockOperationIdMin());
		}
		
		if (filter.getAtLocationId() != null) {
			if (filter.getChildLocations() != null && filter.getChildLocations()) {
				List<Integer> locationIds = getCompleteLocationTree(filter.getAtLocationId()).stream()
				        .map(p -> p.getChildLocationId()).collect(Collectors.toList());
				if (locationIds.isEmpty()) {
					locationIds.add(filter.getAtLocationId());
				}
				if (locationIds.size() == 1) {
					appendFilter(hqlFilter, "so.atLocation.locationId = :atLocationId");
					parameterList.putIfAbsent("atLocationId", locationIds.get(0));
				} else {
					appendFilter(hqlFilter, "so.atLocation.locationId in (:atLocationIds)");
					parameterWithList.putIfAbsent("atLocationIds", locationIds);
				}
			} else {
				appendFilter(hqlFilter, "so.atLocation.locationId = :atLocationId");
				parameterList.putIfAbsent("atLocationId", filter.getAtLocationId());
			}
		}
		
		if (filter.getSourcePartyId() != null) {
			boolean filterSet = false;
			if (filter.getSourcePartyChildLocations() != null && filter.getSourcePartyChildLocations()) {
				Party party = getPartyById(filter.getSourcePartyId());
				if (party == null) {
					return new Result<>(new ArrayList<>(), 0);
				}
				if (party.getLocation() != null) {
					List<Integer> locationIds = getCompleteLocationTree(party.getLocation().getLocationId()).stream()
					        .map(p -> p.getChildLocationId()).collect(Collectors.toList());
					if (locationIds.isEmpty()) {
						locationIds.add(party.getLocation().getLocationId());
					}
					Map<Integer, Integer> partLocationIds = getLocationPartyIds(locationIds);
					if (!partLocationIds.isEmpty()) {
						appendFilter(hqlFilter, "so.source.id in (:sourcePartyIds)");
						parameterWithList.putIfAbsent("sourcePartyIds", partLocationIds.values());
						filterSet = true;
					}
				}
			}
			if (!filterSet) {
				appendFilter(hqlFilter, "so.source.id = :sourcePartyId");
				parameterList.putIfAbsent("sourcePartyId", filter.getSourcePartyId());
			}
		}
		
		if (filter.getDestinationPartyId() != null) {
			boolean filterSet = false;
			if (filter.getDestinationPartyChildLocations() != null && filter.getDestinationPartyChildLocations()) {
				Party party = getPartyById(filter.getDestinationPartyId());
				if (party == null) {
					return new Result<>(new ArrayList<>(), 0);
				}
				if (party.getLocation() != null) {
					List<Integer> locationIds = getCompleteLocationTree(party.getLocation().getLocationId()).stream()
					        .map(p -> p.getChildLocationId()).collect(Collectors.toList());
					if (locationIds.isEmpty()) {
						locationIds.add(party.getLocation().getLocationId());
					}
					Map<Integer, Integer> partLocationIds = getLocationPartyIds(locationIds);
					if (!partLocationIds.isEmpty()) {
						appendFilter(hqlFilter, "so.destination.id in (:destinationPartyIds)");
						parameterWithList.putIfAbsent("destinationPartyIds", partLocationIds.values());
						filterSet = true;
					}
				}
			}
			if (!filterSet) {
				appendFilter(hqlFilter, "so.destination.id = :destinationPartyId");
				parameterList.putIfAbsent("destinationPartyId", filter.getDestinationPartyId());
			}
		}
		
		if (filter.getStockOperationTypes() != null && filter.getStockOperationTypes().size() > 0) {
			if (filter.getStockOperationTypes().size() == 1) {
				appendFilter(hqlFilter, "so.stockOperationType.id = :otid");
				parameterList.putIfAbsent("otid", filter.getStockOperationTypes().get(0).getId());
			} else {
				appendFilter(hqlFilter, "so.stockOperationType.id in (:otid)");
				parameterWithList.putIfAbsent("otid", filter.getStockOperationTypes());
			}
		}
		
		if (filter.getStockOperationStatuses() != null && filter.getStockOperationStatuses().size() > 0) {
			if (filter.getStockOperationStatuses().size() == 1) {
				appendFilter(hqlFilter, "so.status = :status");
				parameterList.putIfAbsent("status", filter.getStockOperationStatuses().get(0));
			} else {
				appendFilter(hqlFilter, "so.status in (:status)");
				parameterWithList.putIfAbsent("status", filter.getStockOperationStatuses());
			}
		}
		
		if (filter.getStartDate() != null) {
			appendFilter(hqlFilter, "so.dateCreated >= :sodcm");
			parameterList.putIfAbsent("sodcm", filter.getStartDate());
		}
		
		if (filter.getEndDate() != null) {
			appendFilter(hqlFilter, "so.dateCreated <= :sodcmx");
			parameterList.putIfAbsent("sodcmx", filter.getEndDate());
		}
		
		appendFilter(hqlFilter, "so.voided = :vdd");
		parameterList.putIfAbsent("vdd", false);
		
		if (filter.getStockOperationItemIdMin() != null) {
			appendFilter(hqlFilter, "soi.id > :stockOperationItemId");
			parameterList.putIfAbsent("stockOperationItemId", filter.getStockOperationItemIdMin());
		}
		
		appendFilter(hqlFilter, "soi.voided = :vdd");
		
		if (filter.getStockItemCategoryConceptId() != null) {
			appendFilter(hqlFilter, "si.category.conceptId = :stockItemCategoryId");
			parameterList.putIfAbsent("stockItemCategoryId", filter.getStockItemCategoryConceptId());
		}
		
		if (hqlFilter.length() > 0) {
			hqlQuery.append(" where ");
			hqlQuery.append(hqlFilter);
		}
		
		Result<StockOperationLineItem> result = new Result<>();
		DbSession dbSession = getSession();
		hqlQuery.append(" order by so.id asc, soi.id asc");
		Query query = dbSession.createQuery(hqlQuery.toString());
		if (parameterList != null) {
			for (Map.Entry<String, Object> entry : parameterList.entrySet())
				query.setParameter(entry.getKey(), entry.getValue());
		}
		if (parameterWithList != null) {
			for (Map.Entry<String, Collection> entry : parameterWithList.entrySet())
				query.setParameterList(entry.getKey(), entry.getValue());
		}
		
		query = query.setResultTransformer(new AliasToBeanResultTransformer(StockOperationLineItem.class));
		if (filter.getStockOperationItemIdMin() != null) {
			query.setFirstResult(0);
		} else {
			query.setFirstResult(filter.getLimit() * filter.getStartIndex());
		}
		query.setMaxResults(filter.getLimit());
		query.setFetchSize(filter.getLimit());
		result.setData(query.list());
		
		if (!result.getData().isEmpty()) {
			List<Integer> conceptIds = new ArrayList<>();
			List<Integer> drugIds = new ArrayList<>();
			
			conceptIds.addAll(result.getData().stream().filter(p -> p.getPackagingUoMId() != null)
			        .map(p -> p.getPackagingUoMId()).collect(Collectors.toList()));
			conceptIds.addAll(result.getData().stream().filter(p -> p.getQuantityRequestedPackagingUoMId() != null)
			        .map(p -> p.getQuantityRequestedPackagingUoMId()).collect(Collectors.toList()));
			conceptIds.addAll(result.getData().stream().filter(p -> p.getReasonId() != null).map(p -> p.getReasonId())
			        .collect(Collectors.toList()));
			conceptIds.addAll(result.getData().stream().filter(p -> p.getStockItemCategoryConceptId() != null)
			        .map(p -> p.getStockItemCategoryConceptId()).collect(Collectors.toList()));
			conceptIds.addAll(result.getData().stream().filter(p -> p.getStockItemConceptId() != null)
			        .map(p -> p.getStockItemConceptId()).collect(Collectors.toList()));
			drugIds.addAll(result.getData().stream().filter(p -> p.getStockItemDrugId() != null)
			        .map(p -> p.getStockItemDrugId()).collect(Collectors.toList()));
			List<Integer> userIds = result.getData().stream().map(p -> p.getCreator()).filter(p -> p != null)
			        .collect(Collectors.toList());
			userIds.addAll(
			    result.getData().stream().map(p -> p.getCompletedBy()).filter(p -> p != null).collect(Collectors.toList()));
			userIds.addAll(result.getData().stream().map(p -> p.getResponsiblePerson()).filter(p -> p != null)
			        .collect(Collectors.toList()));
			
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
			
			for (StockOperationLineItem stockOperationItemDTO : result.getData()) {
				
				List<ConceptNameDTO> conceptNameDTO = null;
				if (stockOperationItemDTO.getReasonId() != null) {
					conceptNameDTO = conceptNameDTOs.get(stockOperationItemDTO.getReasonId());
					if (conceptNameDTO != null) {
						stockOperationItemDTO.setReasonName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (stockOperationItemDTO.getPackagingUoMId() != null) {
					conceptNameDTO = conceptNameDTOs.get(stockOperationItemDTO.getPackagingUoMId());
					if (conceptNameDTO != null) {
						stockOperationItemDTO.setStockItemPackagingUOMName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (stockOperationItemDTO.getQuantityRequestedPackagingUoMId() != null) {
					conceptNameDTO = conceptNameDTOs.get(stockOperationItemDTO.getQuantityRequestedPackagingUoMId());
					if (conceptNameDTO != null) {
						stockOperationItemDTO.setQuantityRequestedPackagingUOMName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (stockOperationItemDTO.getStockItemCategoryConceptId() != null) {
					conceptNameDTO = conceptNameDTOs.get(stockOperationItemDTO.getStockItemCategoryConceptId());
					if (conceptNameDTO != null) {
						stockOperationItemDTO.setStockItemCategoryName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (stockOperationItemDTO.getStockItemConceptId() != null) {
					conceptNameDTO = conceptNameDTOs.get(stockOperationItemDTO.getStockItemConceptId());
					if (conceptNameDTO != null) {
						stockOperationItemDTO.setStockItemConceptName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (stockOperationItemDTO.getStockItemDrugId() != null) {
					conceptNameDTO = drugNames.get(stockOperationItemDTO.getStockItemDrugId());
					if (conceptNameDTO != null) {
						stockOperationItemDTO.setStockItemDrugName(conceptNameDTO.get(0).getName());
					}
				}
				
				if (stockOperationItemDTO.getCreator() != null) {
					List<UserPersonNameDTO> userPersonNameDTO = personNames.get(stockOperationItemDTO.getCreator());
					if (userPersonNameDTO != null) {
						stockOperationItemDTO.setCreatorFamilyName(userPersonNameDTO.get(0).getFamilyName());
						stockOperationItemDTO.setCreatorGivenName(userPersonNameDTO.get(0).getGivenName());
					}
				}
				
				if (stockOperationItemDTO.getCompletedBy() != null) {
					List<UserPersonNameDTO> userPersonNameDTO = personNames.get(stockOperationItemDTO.getCompletedBy());
					if (userPersonNameDTO != null) {
						stockOperationItemDTO.setCompletedByFamilyName(userPersonNameDTO.get(0).getFamilyName());
						stockOperationItemDTO.setCompletedByGivenName(userPersonNameDTO.get(0).getGivenName());
					}
				}
				
				if (stockOperationItemDTO.getResponsiblePerson() != null) {
					List<UserPersonNameDTO> userPersonNameDTO = personNames
					        .get(stockOperationItemDTO.getResponsiblePerson());
					if (userPersonNameDTO != null) {
						stockOperationItemDTO.setResponsiblePersonFamilyName(userPersonNameDTO.get(0).getFamilyName());
						stockOperationItemDTO.setResponsiblePersonGivenName(userPersonNameDTO.get(0).getGivenName());
					}
				}
			}
			
		}
		
		return result;
	}
	
	public String getUserEmail(Integer userId) {
		try {
			DbSession session = getSession();
			Query query = session.createSQLQuery("SELECT email FROM users WHERE user_id = :p");
			query.setParameter("p", userId);
			List result = query.list();
			if (result.isEmpty())
				return null;
			String emailAddress = (String) result.get(0);
			if (emailAddress == null)
				return null;
			return emailAddress;
		}
		catch (Exception exception) {}
		return null;
	}
	
	public List<BatchJob> getExpiredBatchJobs() {
		return getSession().createCriteria(BatchJob.class).add(Restrictions.le("expiration", new Date())).list();
	}
	
	public void deleteBatchJob(BatchJob batchJob) {
		DbSession session = getSession();
		Query query = session.createQuery("DELETE FROM stockmanagement.BatchJobOwner WHERE batchJob = :p");
		query.setParameter("p", batchJob);
		query.executeUpdate();
		
		query = session.createQuery("DELETE FROM stockmanagement.BatchJob WHERE id = :p");
		query.setParameter("p", batchJob.getId());
		query.executeUpdate();
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
