/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.stockmanagement.api.dao;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.api.db.hibernate.search.session.SearchSessionFactory;
import org.openmrs.module.stockmanagement.api.IPagingInfo;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DaoBase {
	
	protected DbSessionFactory sessionFactory;

	protected SearchSessionFactory searchSessionFactory;
	
	public void setSessionFactory(DbSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setSearchSessionFactory(SearchSessionFactory searchSessionFactory) {
		this.searchSessionFactory = searchSessionFactory;
	}

	public Session getSession() {
		return sessionFactory.getHibernateSessionFactory().getCurrentSession();
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
				
				Session dbSession = getSession();
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
		
		Session dbSession = getSession();
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
		
		if (dtoClass != null) {
			query = query.setResultTransformer(new AliasToBeanResultTransformer(dtoClass));
		}
		
		if (pagingInfo != null && pagingInfo.getPageIndex() != null && pagingInfo.getPageSize() != null
		        && pagingInfo.getPageIndex() >= 0 && pagingInfo.getPageSize() >= 0) {
			query.setFirstResult((pagingInfo.getPageIndex()) * pagingInfo.getPageSize());
			query.setMaxResults(pagingInfo.getPageSize());
			query.setFetchSize(pagingInfo.getPageSize());
		}
		return query;
	}
	
	protected void appendFilter(StringBuilder stringBuilder, String filter) {
		if (!stringBuilder.isEmpty())
			stringBuilder.append(" AND ");
		stringBuilder.append("(");
		stringBuilder.append(filter);
		stringBuilder.append(")");
	}
	
	protected void appendORFilter(StringBuilder stringBuilder, String filter) {
		if (!stringBuilder.isEmpty())
			stringBuilder.append(" OR ");
		stringBuilder.append("(");
		stringBuilder.append(filter);
		stringBuilder.append(")");
	}
	
}
