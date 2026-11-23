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

import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.stockmanagement.api.model.StockSource;
import org.openmrs.module.stockmanagement.api.search.StockSourceSearch;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface StockSourceService extends OpenmrsService {
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKSOURCES)
	StockSource getStockSourceByUuid(String uuid);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKSOURCES)
	List<StockSource> getStockSources(StockSourceSearch search);
	
	@Transactional(readOnly = true)
	@Authorized(Privileges.APP_STOCKMANAGEMENT_STOCKSOURCES)
	List<StockSource> getStockSources(StockSourceSearch search, IPagingInfo pagingInfo);
	
	@Transactional
	@Authorized(Privileges.TASK_STOCKMANAGEMENT_STOCKSOURCES_MUTATE)
	StockSource saveStockSource(StockSource stockSource);
	
	@Transactional
	@Authorized(Privileges.TASK_STOCKMANAGEMENT_STOCKSOURCES_MUTATE)
	void voidStockSources(List<String> stockSourceIds, String reason, int voidedBy);
}
