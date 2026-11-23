/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.db;

import org.openmrs.module.stockmanagement.api.IPagingInfo;
import org.openmrs.module.stockmanagement.api.model.StockSource;
import org.openmrs.module.stockmanagement.api.search.StockSourceSearch;

import java.util.List;

public interface StockSourceDAO {
	
	StockSource getStockSourceByUuid(String uuid);
	
	List<StockSource> getStockSources(StockSourceSearch search, IPagingInfo pagingInfo);
	
	StockSource saveStockSource(StockSource stockSource);
	
	void voidStockSources(List<String> stockSourceIds, String reason, int voidedBy);
}
