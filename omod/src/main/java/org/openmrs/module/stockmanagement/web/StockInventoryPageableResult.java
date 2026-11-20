/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.web;

import org.openmrs.module.stockmanagement.api.dto.StockInventoryResult;
import org.openmrs.module.stockmanagement.api.dto.StockItemInventory;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.Hyperlink;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.resource.api.Converter;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.util.ArrayList;
import java.util.List;

public class StockInventoryPageableResult extends AlreadyPaged<StockItemInventory> {
	
	private List<StockItemInventory> totals = null;
	
	public StockInventoryPageableResult(RequestContext context, List<StockItemInventory> results, boolean hasMoreResults) {
		super(context, results, hasMoreResults);
	}
	
	public StockInventoryPageableResult(RequestContext context, List<StockItemInventory> results, boolean hasMoreResults,
	    Long totalCount) {
		super(context, results, hasMoreResults, totalCount);
	}
	
	public StockInventoryPageableResult(RequestContext context, StockInventoryResult results, boolean hasMoreResults) {
		super(context, results.getData(), hasMoreResults);
		totals = results.getTotals();
	}
	
	public StockInventoryPageableResult(RequestContext context, StockInventoryResult results, boolean hasMoreResults,
	    Long totalCount) {
		super(context, results.getData(), hasMoreResults, totalCount);
		totals = results.getTotals();
	}
	
	@Override
	public SimpleObject toSimpleObject(Converter preferredConverter) throws ResponseException {
		SimpleObject result = super.toSimpleObject(preferredConverter);
		result.add("total", totals);
		return result;
	}
}
