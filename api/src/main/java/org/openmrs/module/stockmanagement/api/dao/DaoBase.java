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

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.transform.ResultTransformer;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.api.db.hibernate.search.session.SearchSessionFactory;
import org.openmrs.module.stockmanagement.api.IPagingInfo;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Location;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.ConceptNameType;
import org.openmrs.module.stockmanagement.api.dto.ConceptNameDTO;
import org.openmrs.module.stockmanagement.api.dto.PartyDTO;
import org.openmrs.module.stockmanagement.api.dto.PrivilegeScope;
import org.openmrs.module.stockmanagement.api.dto.UserPersonNameDTO;
import org.openmrs.module.stockmanagement.api.model.Party;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;
import org.openmrs.module.stockmanagement.api.utils.DateUtil;

import org.apache.commons.lang.StringUtils;
import org.openmrs.module.stockmanagement.api.dto.PartySearchFilter;
import org.openmrs.module.stockmanagement.api.dto.Result;
import org.openmrs.module.stockmanagement.api.dto.StockItemDTO;
import org.openmrs.module.stockmanagement.api.dto.StockItemSearchFilter;
import org.openmrs.module.stockmanagement.api.model.LocationTree;
import org.openmrs.module.stockmanagement.api.model.StockItemPackagingUOM;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.Set;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({ "unchecked" })
public class DaoBase {
	
	protected DbSessionFactory sessionFactory;
	
	protected SearchSessionFactory searchSessionFactory;
	
	public void setSessionFactory(DbSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public void setSearchSessionFactory(SearchSessionFactory searchSessionFactory) {
		this.searchSessionFactory = searchSessionFactory;
	}
	
	public DbSession getSession() {
		return sessionFactory.getCurrentSession();
	}
	
	/**
	 * Gets the current hibernate session while taking care of the hibernate 3 and 4 differences.
	 * 
	 * @return the current hibernate session.
	 */
	protected org.hibernate.Session getCurrentHibernateSession() {
		try {
			return sessionFactory.getHibernateSessionFactory().getCurrentSession();
		}
		catch (NoSuchMethodError ex) {
			try {
				Object hibernateSessionFactory = sessionFactory.getHibernateSessionFactory();
				Method method = hibernateSessionFactory.getClass().getMethod("getCurrentSession", null);
				return (org.hibernate.Session) method.invoke(hibernateSessionFactory, null);
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to get the hibernate session", e);
			}
		}
	}
	
	protected org.hibernate.StatelessSession getStatelessHibernateSession() {
		try {
			return sessionFactory.getHibernateSessionFactory().openStatelessSession();
		}
		catch (NoSuchMethodError ex) {
			try {
				Object hibernateSessionFactory = sessionFactory.getHibernateSessionFactory();
				Method method = hibernateSessionFactory.getClass().getMethod("openStatelessSession", null);
				return (org.hibernate.StatelessSession) method.invoke(hibernateSessionFactory, null);
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to get the hibernate stateless session", e);
			}
		}
	}
	
	@SuppressWarnings({ "unchecked" })
	protected <T> List<T> executeCriteria(Criteria criteria, IPagingInfo pagingInfo, Order... orderBy) {
		
		loadPagingTotal(pagingInfo, criteria);
		
		if (orderBy != null && orderBy.length > 0) {
			for (Order order : orderBy) {
				criteria.addOrder(order);
			}
		}
		return createPagingCriteria(pagingInfo, criteria).list();
	}
	
	/**
	 * Loads the record count for the specified criteria into the specified paging object.
	 * 
	 * @param pagingInfo The {@link IPagingInfo} object to load with the record count.
	 * @param criteria The {@link Criteria} to execute against the hibernate data source or {@code null}
	 *            to create a new one.
	 */
	protected void loadPagingTotal(IPagingInfo pagingInfo, Criteria criteria) {
		if (pagingInfo != null && pagingInfo.getPageIndex() != null && pagingInfo.getPageSize() != null
		        && pagingInfo.getPageIndex() >= 0 && pagingInfo.getPageSize() >= 0) {
			if (criteria == null) {
				return;
			}
			
			if (pagingInfo.shouldLoadRecordCount()) {
				// Copy the current projection and transformer which requires getting access to the underlying criteria
				// implementation
				Projection projection = null;
				ResultTransformer transformer = null;
				
				if (criteria instanceof CriteriaImpl) {
					CriteriaImpl impl = (CriteriaImpl) criteria;
					projection = impl.getProjection();
					transformer = impl.getResultTransformer();
				}
				
				try {
					criteria.setProjection(Projections.rowCount());
					
					Long count = (Long) criteria.uniqueResult();
					pagingInfo.setTotalRecordCount(count == null ? 0 : count);
					pagingInfo.setLoadRecordCount(false);
				}
				finally {
					// Reset the criteria projection and transformer to return the result rather than the row count
					criteria.setProjection(projection);
					criteria.setResultTransformer(transformer);
				}
			}
		}
	}
	
	/**
	 * Updates the specified {@link Criteria} object to retrieve the data specified by the
	 * {@link IPagingInfo} object.
	 * 
	 * @param pagingInfo The {@link IPagingInfo} object that specifies which data should be retrieved.
	 * @param criteria The {@link Criteria} to add the paging settings to, or {@code null} to create a
	 *            new one.
	 * @return The {@link Criteria} object with the paging settings applied.
	 */
	protected Criteria createPagingCriteria(IPagingInfo pagingInfo, Criteria criteria) {
		if (criteria != null && pagingInfo != null && pagingInfo.getPageIndex() != null && pagingInfo.getPageSize() != null
		        && pagingInfo.getPageIndex() >= 0 && pagingInfo.getPageSize() >= 0) {
			criteria.setFirstResult((pagingInfo.getPageIndex()) * pagingInfo.getPageSize());
			criteria.setMaxResults(pagingInfo.getPageSize());
			criteria.setFetchSize(pagingInfo.getPageSize());
		}
		return criteria;
	}
	
	@SuppressWarnings({ "unchecked" })
	protected <T> List<T> executeQuery(Class dtoClass, StringBuilder query, IPagingInfo pagingInfo, String order,
	        HashMap<String, Object> parameters, HashMap<String, Collection> parametersWithList) {
		
		loadPagingTotal(pagingInfo, query, parameters, parametersWithList);
		
		return createPagingQuery(pagingInfo, dtoClass, query, order, parameters, parametersWithList).list();
	}
	
	protected void loadPagingTotal(IPagingInfo pagingInfo, StringBuilder query, HashMap<String, Object> parameters,
	        HashMap<String, Collection> parametersWithList) {
		if (pagingInfo != null && pagingInfo.getPageIndex() != null && pagingInfo.getPageSize() != null
		        && pagingInfo.getPageIndex() >= 0 && pagingInfo.getPageSize() >= 0) {
			if (query == null) {
				return;
			}
			
			if (pagingInfo.shouldLoadRecordCount()) {
				// Copy the current projection and transformer which requires getting access to the underlying criteria
				// implementation
				
				DbSession dbSession = getSession();
				Query queryCount = dbSession.createQuery(getCountQuery(query.toString()));
				if (parameters != null) {
					for (Map.Entry<String, Object> entry : parameters.entrySet())
						queryCount.setParameter(entry.getKey(), entry.getValue());
				}
				if (parametersWithList != null) {
					for (Map.Entry<String, Collection> entry : parametersWithList.entrySet())
						queryCount.setParameterList(entry.getKey(), entry.getValue());
				}
				
				Long count = (Long) queryCount.uniqueResult();
				pagingInfo.setTotalRecordCount(count == null ? 0 : count);
				pagingInfo.setLoadRecordCount(false);
			}
		}
	}
	
	private String getCountQuery(String query) {
		String lowerQuery = query.toLowerCase();
		int indexOfSelect = lowerQuery.indexOf("select");
		int indexOfFrom = lowerQuery.indexOf("from");
		boolean foundSegments = false;
		while (indexOfFrom > indexOfSelect && indexOfFrom > 0) {
			int charIndexAfterFrom = indexOfFrom + 4;
			if (Character.isWhitespace(lowerQuery.charAt(indexOfFrom - 1)) && charIndexAfterFrom < lowerQuery.length()
			        && Character.isWhitespace(lowerQuery.charAt(charIndexAfterFrom))) {
				foundSegments = true;
				break;
			}
			indexOfFrom = lowerQuery.indexOf("from", indexOfFrom + 1);
		}
		if (!foundSegments)
			throw new RuntimeException("Failed to parse query to generate count query");
		
		return "select count(*) " + query.substring(indexOfFrom);
	}
	
	protected Query createPagingQuery(IPagingInfo pagingInfo, Class dtoClass, StringBuilder hqlQuery, String order,
	        HashMap<String, Object> parameters, HashMap<String, Collection> parametersWithList) {
		if (hqlQuery == null) {
			return null;
		}
		
		DbSession dbSession = getSession();
		StringBuilder stringBuilder = new StringBuilder(hqlQuery);
		if (order != null)
			stringBuilder.append(order);
		Query query = dbSession.createQuery(stringBuilder.toString());
		if (parameters != null) {
			for (Map.Entry<String, Object> entry : parameters.entrySet())
				query.setParameter(entry.getKey(), entry.getValue());
		}
		if (parametersWithList != null) {
			for (Map.Entry<String, Collection> entry : parametersWithList.entrySet())
				query.setParameterList(entry.getKey(), entry.getValue());
		}
		
		query = query.setResultTransformer(new AliasToBeanResultTransformer(dtoClass));
		
		if (pagingInfo != null && pagingInfo.getPageIndex() != null && pagingInfo.getPageSize() != null
		        && pagingInfo.getPageIndex() >= 0 && pagingInfo.getPageSize() >= 0) {
			query.setFirstResult((pagingInfo.getPageIndex()) * pagingInfo.getPageSize());
			query.setMaxResults(pagingInfo.getPageSize());
			query.setFetchSize(pagingInfo.getPageSize());
		}
		return query;
	}
	
	protected void appendFilter(StringBuilder stringBuilder, String filter) {
		if (stringBuilder.length() > 0)
			stringBuilder.append(" AND ");
		stringBuilder.append("(");
		stringBuilder.append(filter);
		stringBuilder.append(")");
	}
	
	protected void appendORFilter(StringBuilder stringBuilder, String filter) {
		if (stringBuilder.length() > 0)
			stringBuilder.append(" OR ");
		stringBuilder.append("(");
		stringBuilder.append(filter);
		stringBuilder.append(")");
	}
	
	public Map<Integer, String> getConceptNames(List<Integer> conceptIds) {
		if (conceptIds == null || conceptIds.isEmpty())
			return new HashMap<>();
		Query query = getSession().createQuery(
		    "select cc.concept.conceptId as conceptId, cc.name as name from ConceptName cc where cc.concept.conceptId in (:ids) and cc.conceptNameType = (:cnt)")
		        .setParameterList("ids", conceptIds).setParameter("cnt", ConceptNameType.FULLY_SPECIFIED);
		List result = query.list();
		Map<Integer, String> resultMap = new HashMap<>();
		for (Object object : result) {
			resultMap.putIfAbsent(Integer.valueOf(((Number) (((Object[]) object)[0])).intValue()),
			    (String) (((Object[]) object)[1]));
		}
		return resultMap;
	}
	
	public List<ConceptNameDTO> getConceptNamesByConceptIds(List<Integer> ids) {
		if (ids == null || ids.isEmpty())
			return new ArrayList<>();
		Query query = getSession().createQuery(
		    "select cc.concept.conceptId as conceptId, cc.name as name, cc.locale as locale from ConceptName cc where cc.concept.conceptId in (:ids) and cc.conceptNameType = (:cnt)")
		        .setParameterList("ids", ids).setParameter("cnt", ConceptNameType.FULLY_SPECIFIED);
		query = query.setResultTransformer(new AliasToBeanResultTransformer(ConceptNameDTO.class));
		return query.list();
	}
	
	public List<ConceptNameDTO> getDrugNamesByDrugIds(List<Integer> ids) {
		if (ids == null || ids.isEmpty())
			return new ArrayList<>();
		Query query = getSession()
		        .createQuery("select d.drugId as conceptId, d.name as name from Drug d where d.drugId in (:ids)")
		        .setParameterList("ids", ids);
		query = query.setResultTransformer(new AliasToBeanResultTransformer(ConceptNameDTO.class));
		return query.list();
	}
	
	protected List<UserPersonNameDTO> getPatientNameByPatientIds(List<Integer> ids, boolean includePatientIdentifier) {
		if (ids == null || ids.isEmpty())
			return new ArrayList<>();
		Query query = sessionFactory.getCurrentSession().createSQLQuery(
		    "select p.uuid as uuid, up.person_id as patientId, p.given_name as givenName, p.middle_name as middleName, p.family_name as familyName "
		            + (includePatientIdentifier
		                    ? ",(select pi.identifier from patient_identifier pi where pi.patient_id = up.person_id order by pi.preferred desc, pi.patient_identifier_id asc limit 1) as patientIdentifier"
		                    : "")
		            + " from person up join person_name p on up.person_id = p.person_id where up.person_id in (:ids)")
		        .setParameterList("ids", ids);
		query = query.setResultTransformer(new AliasToBeanResultTransformer(UserPersonNameDTO.class));
		return query.list();
	}
	
	public List<PrivilegeScope> getFlattenedUserRoleScopesByUser(User user, Set<Role> roles, Location location,
	        StockOperationType stockOperationType) {
		if (user == null || roles.size() == 0)
			return new ArrayList<>();
		StringBuilder hqlQuery = new StringBuilder("SELECT DISTINCT urs.role.role as role, l.uuid as locationUuid,"
		        + " p.uuid as partyUuid, sot.uuid as operationTypeUuid, urs.permanent as isPermanent, "
		        + "urs.activeFrom as activeFrom, urs.activeTo as activeTo\n"
		        + "FROM stockmanagement.UserRoleScope urs join     \n" + "     urs.userRoleScopeLocations ursl join\n"
		        + "     urs.userRoleScopeOperationTypes ursot join\n" + "     ursot.stockOperationType sot, \n"
		        + "     stockmanagement.LocationTree lt, \n" + "     Location l, \n" + "     stockmanagement.Party p \n"
		        + "WHERE ursl.enableDescendants = 1 AND ursl.location.id = lt.parentLocationId AND lt.childLocationId = l.id AND lt.childLocationId = p.location.id AND urs.user = :user AND urs.role in (:roles) AND urs.enabled = 1 AND urs.voided = 0 AND "
		        + "(urs.permanent = 1 OR (urs.activeFrom <= :from AND urs.activeTo >= :to )) and ursl.voided=0 and ursot.voided=0");
		
		HashMap<String, Object> parameterList = new HashMap<>();
		if (location != null) {
			appendFilter(hqlQuery, "ursl.location = :location or lt.childLocationId = :location");
			parameterList.putIfAbsent("location", location);
		}
		
		if (stockOperationType != null) {
			appendFilter(hqlQuery, "ursot.stockOperationType = :stockOperationType");
			parameterList.putIfAbsent("stockOperationType", stockOperationType);
		}
		
		Query query = getSession().createQuery(hqlQuery.toString());
		query.setParameter("user", user);
		query.setParameterList("roles", roles);
		query.setParameter("from", DateUtil.today());
		query.setParameter("to", DateUtil.today());
		for (Map.Entry<String, Object> entry : parameterList.entrySet())
			query.setParameter(entry.getKey(), entry.getValue());
		
		query = query.setResultTransformer(new AliasToBeanResultTransformer(PrivilegeScope.class));
		List<PrivilegeScope> result = query.list();
		
		hqlQuery = new StringBuilder("SELECT DISTINCT urs.role.role as role, l.uuid as locationUuid,"
		        + " p.uuid as partyUuid, sot.uuid as operationTypeUuid, urs.permanent as isPermanent, "
		        + "urs.activeFrom as activeFrom, urs.activeTo as activeTo\n"
		        + "FROM stockmanagement.UserRoleScope urs join     \n" + "     urs.userRoleScopeLocations ursl join\n"
		        + "     urs.userRoleScopeOperationTypes ursot join\n" + "     ursot.stockOperationType sot, \n"
		        + "     Location l, \n" + "     stockmanagement.Party p \n"
		        + "WHERE ursl.enableDescendants = 0 AND ursl.location.id = l.id AND ursl.location.id = p.location.id AND urs.user = :user AND urs.role in (:roles) AND urs.enabled = 1 AND urs.voided = 0 AND "
		        + "(urs.permanent = 1 OR (urs.activeFrom <= :from AND urs.activeTo >= :to )) and ursl.voided=0 and ursot.voided=0");
		
		parameterList = new HashMap<>();
		if (location != null) {
			appendFilter(hqlQuery, "ursl.location = :location");
			parameterList.putIfAbsent("location", location);
		}
		
		if (stockOperationType != null) {
			appendFilter(hqlQuery, "ursot.stockOperationType = :stockOperationType");
			parameterList.putIfAbsent("stockOperationType", stockOperationType);
		}
		
		query = getSession().createQuery(hqlQuery.toString());
		query.setParameter("user", user);
		query.setParameterList("roles", roles);
		query.setParameter("from", DateUtil.today());
		query.setParameter("to", DateUtil.today());
		for (Map.Entry<String, Object> entry : parameterList.entrySet())
			query.setParameter(entry.getKey(), entry.getValue());
		
		query = query.setResultTransformer(new AliasToBeanResultTransformer(PrivilegeScope.class));
		result.addAll(query.list());
		return result;
	}
	
	public Map<Integer, String> getPartyNames(List<Integer> partyIds) {
		if (partyIds == null || partyIds.isEmpty())
			return new HashMap<>();
		StringBuilder hqlQuery = new StringBuilder("SELECT distinct p.id as id," + " coalesce(l.name, ss.name) as name\n"
		        + " FROM stockmanagement.Party p left join\n" + " p.location l left join\n" + " p.stockSource ss"
		        + " where p.id in (:pids)");
		Query query = getSession().createQuery(hqlQuery.toString());
		query.setParameterList("pids", partyIds);
		query = query.setResultTransformer(new AliasToBeanResultTransformer(PartyDTO.class));
		return (Map<Integer, String>) query.list().stream().collect(Collectors.toMap(PartyDTO::getId, PartyDTO::getName));
	}
	
	public Map<Integer, Integer> getLocationPartyIds(Collection<Integer> locationIds) {
		if (locationIds == null || locationIds.isEmpty())
			return new HashMap<>();
		Criteria criteria = getSession().createCriteria(Party.class)
		        .add(Restrictions.in("location.locationId", locationIds));
		Projection projection1 = Projections.property("location.locationId");
		Projection projection2 = Projections.property("id");
		ProjectionList pList = Projections.projectionList();
		pList.add(projection1);
		pList.add(projection2);
		criteria.setProjection(pList);
		List result = criteria.list();
		HashMap<Integer, Integer> ids = new HashMap<>();
		for (Object object : result) {
			Object[] row = (Object[]) object;
			ids.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
		}
		return ids;
	}
	
	public Map<String, Integer> getPartyIds(Collection<String> uuids) {
		if (uuids == null || uuids.isEmpty())
			return new HashMap<>();
		Criteria criteria = getSession().createCriteria(Party.class).add(Restrictions.in("uuid", uuids));
		Projection projection1 = Projections.property("id");
		Projection projection2 = Projections.property("uuid");
		ProjectionList pList = Projections.projectionList();
		pList.add(projection2);
		pList.add(projection1);
		criteria.setProjection(pList);
		List result = criteria.list();
		HashMap<String, Integer> ids = new HashMap<>();
		for (Object object : result) {
			Object[] row = (Object[]) object;
			ids.put((String) row[0], ((Number) row[1]).intValue());
		}
		return ids;
	}
	
	public Map<String, Integer> getStockItemIds(Collection<String> uuids) {
		if (uuids == null || uuids.isEmpty())
			return new HashMap<>();
		Criteria criteria = getSession().createCriteria(StockItem.class).add(Restrictions.in("uuid", uuids));
		Projection projection1 = Projections.property("id");
		Projection projection2 = Projections.property("uuid");
		ProjectionList pList = Projections.projectionList();
		pList.add(projection2);
		pList.add(projection1);
		criteria.setProjection(pList);
		List result = criteria.list();
		HashMap<String, Integer> ids = new HashMap<>();
		for (Object object : result) {
			Object[] row = (Object[]) object;
			ids.put((String) row[0], ((Number) row[1]).intValue());
		}
		return ids;
	}
	
	public Map<String, Integer> getLocationIds(Collection<String> uuids) {
		if (uuids == null || uuids.isEmpty())
			return new HashMap<>();
		Criteria criteria = getSession().createCriteria(Location.class).add(Restrictions.in("uuid", uuids));
		Projection projection1 = Projections.property("locationId");
		Projection projection2 = Projections.property("uuid");
		ProjectionList pList = Projections.projectionList();
		pList.add(projection2);
		pList.add(projection1);
		criteria.setProjection(pList);
		List result = criteria.list();
		HashMap<String, Integer> ids = new HashMap<>();
		for (Object object : result) {
			Object[] row = (Object[]) object;
			ids.put((String) row[0], ((Number) row[1]).intValue());
		}
		return ids;
	}
	
	public List<LocationTree> getCompleteLocationTree(Integer atLocationId) {
		Criteria criteria = getSession().createCriteria(LocationTree.class);
		criteria.add(Restrictions.eq("parentLocationId", atLocationId));
		return criteria.list();
	}
	
	public StockItemPackagingUOM getStockItemPackagingUOMByUuid(String uuid) {
		return (StockItemPackagingUOM) getSession().createCriteria(StockItemPackagingUOM.class)
		        .add(Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	protected boolean isNotNullOrEmpty(String value) {
		return value != null && !value.isEmpty();
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
	
	public List<UserPersonNameDTO> getPersonNameByUserIds(List<Integer> ids) {
		if (ids == null || ids.isEmpty())
			return new ArrayList<>();
		Query query = sessionFactory.getCurrentSession().createQuery(
		    "select u.uuid as uuid, u.userId as userId, p.givenName as givenName, p.middleName as middleName, p.familyName as familyName from User u join u.person up join up.names p where u.userId in (:ids)")
		        .setParameterList("ids", ids);
		query = query.setResultTransformer(new AliasToBeanResultTransformer(UserPersonNameDTO.class));
		return query.list();
	}
	
	public Party getPartyByLocation(Location location) {
		if (location == null)
			return null;
		Criteria criteria = getSession().createCriteria(Party.class);
		criteria.add(Restrictions.eq("location", location));
		return (Party) criteria.uniqueResult();
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
}
