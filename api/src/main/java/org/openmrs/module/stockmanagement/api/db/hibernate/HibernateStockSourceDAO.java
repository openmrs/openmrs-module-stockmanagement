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

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.module.stockmanagement.api.IPagingInfo;
import org.openmrs.module.stockmanagement.api.dao.DaoBase;
import org.openmrs.module.stockmanagement.api.db.StockSourceDAO;
import org.openmrs.module.stockmanagement.api.model.StockSource;
import org.openmrs.module.stockmanagement.api.search.StockSourceSearch;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class HibernateStockSourceDAO extends DaoBase implements StockSourceDAO {
	
	@Override
	public StockSource getStockSourceByUuid(String uuid) {
		if (StringUtils.isBlank(uuid)) {
			return null;
		}
		return (StockSource) getSession().createCriteria(StockSource.class).add(Restrictions.eq("uuid", uuid))
		        .uniqueResult();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<StockSource> getStockSources(StockSourceSearch search, IPagingInfo pagingInfo) {
		DbSession session = getSession();
		Criteria criteria = session.createCriteria(StockSource.class, "ss");
		
		if (search != null) {
			if (StringUtils.isNotBlank(search.getUuid())) {
				criteria.add(Restrictions.eq("ss.uuid", search.getUuid()));
			}
			
			if (search.getSourceType() != null) {
				criteria.add(Restrictions.eq("ss.sourceType", search.getSourceType()));
			}
			
			if (!Boolean.TRUE.equals(search.getIncludeVoided())) {
				criteria.add(Restrictions.eq("ss.voided", false));
			}
			
			if (StringUtils.isNotBlank(search.getSearch())) {
				String textSearch = search.getSearch().replaceAll("%", "");
				if (textSearch.isEmpty()) {
					if (pagingInfo != null) {
						pagingInfo.setTotalRecordCount(0L);
					}
					return Collections.emptyList();
				}
				textSearch = textSearch + "%";
				criteria.add(Restrictions.or(Restrictions.ilike("ss.name", textSearch),
				    Restrictions.ilike("ss.acronym", textSearch)));
			}
		} else {
			criteria.add(Restrictions.eq("ss.voided", false));
		}
		
		if (pagingInfo != null) {
			return executeCriteria(criteria, pagingInfo);
		}
		
		return criteria.list();
	}
	
	@Override
	public StockSource saveStockSource(StockSource stockSource) {
		getSession().saveOrUpdate(stockSource);
		return stockSource;
	}
	
	@Override
	public void voidStockSources(List<String> stockSourceIds, String reason, int voidedBy) {
		if (stockSourceIds == null || stockSourceIds.isEmpty()) {
			return;
		}
		DbSession session = getSession();
		Query query = session.createQuery(
		    "UPDATE stockmanagement.StockSource SET voided=1, dateVoided=:dateVoided, voidedBy=:voidedBy, voidReason=:reason WHERE uuid in (:uuidList)");
		query.setParameterList("uuidList", stockSourceIds);
		query.setDate("dateVoided", new Date());
		query.setInteger("voidedBy", voidedBy);
		query.setString("reason", reason);
		query.executeUpdate();
	}
}
