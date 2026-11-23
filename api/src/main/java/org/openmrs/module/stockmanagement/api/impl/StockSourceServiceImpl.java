/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.impl;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.stockmanagement.api.IPagingInfo;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.StockSourceService;
import org.openmrs.module.stockmanagement.api.db.StockSourceDAO;
import org.openmrs.module.stockmanagement.api.model.Party;
import org.openmrs.module.stockmanagement.api.model.StockSource;
import org.openmrs.module.stockmanagement.api.search.StockSourceSearch;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class StockSourceServiceImpl extends BaseOpenmrsService implements StockSourceService {
	
	private StockSourceDAO dao;
	
	public void setDao(StockSourceDAO dao) {
		this.dao = dao;
	}
	
	public StockSourceDAO getDao() {
		return dao;
	}
	
	@Override
	@Transactional(readOnly = true)
	public StockSource getStockSourceByUuid(String uuid) {
		if (StringUtils.isBlank(uuid)) {
			return null;
		}
		return dao.getStockSourceByUuid(uuid);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<StockSource> getStockSources(StockSourceSearch search) {
		return getStockSources(search, null);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<StockSource> getStockSources(StockSourceSearch search, IPagingInfo pagingInfo) {
		if (search == null && pagingInfo == null) {
			return Collections.emptyList();
		}
		return dao.getStockSources(search, pagingInfo);
	}
	
	@Override
	@Transactional
	public StockSource saveStockSource(StockSource stockSource) {
		if (stockSource == null) {
			throw new IllegalArgumentException("stockSource cannot be null");
		}
		if (stockSource.getId() != null && stockSource.getId() > 0) {
			stockSource.setChangedBy(Context.getAuthenticatedUser());
			stockSource.setDateChanged(new Date());
		}
		StockSource result = dao.saveStockSource(stockSource);
		Party party = Context.getService(StockManagementService.class).getPartyByStockSource(result);
		if (party == null) {
			party = new Party();
			party.setStockSource(result);
			party.setCreator(Context.getAuthenticatedUser());
			party.setDateCreated(new Date());
			Context.getService(StockManagementService.class).saveParty(party);
		}
		return result;
	}
	
	@Override
	@Transactional
	public void voidStockSources(List<String> stockSourceIds, String reason, int voidedBy) {
		if (stockSourceIds == null || stockSourceIds.isEmpty()) {
			return;
		}
		dao.voidStockSources(stockSourceIds, reason, voidedBy);
	}
}
