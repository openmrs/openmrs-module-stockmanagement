/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.web.resource;

import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.*;
import io.swagger.models.properties.StringProperty;
import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.ModuleConstants;
import org.openmrs.module.stockmanagement.api.StockItemService;
import org.openmrs.module.stockmanagement.api.dto.*;
import org.openmrs.module.stockmanagement.api.model.*;
import org.openmrs.module.stockmanagement.api.search.StockItemSearch;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

@Resource(name = RestConstants.VERSION_1 + "/" + ModuleConstants.MODULE_ID
        + "/stockbatch", supportedClass = StockBatch.class, supportedOpenmrsVersions = { "1.9.*", "1.10.*", "1.11.*",
                "1.12.*", "2.*" })
public class StockBatchResource extends ResourceBase<StockBatch> {
	
	@Override
	public StockBatch getByUniqueId(String uniqueId) {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	protected void delete(StockBatch delegate, String reason, RequestContext context) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	protected PageableResult doSearch(RequestContext context) {
		StockItemSearch filter = new StockItemSearch();
		String param = context.getParameter("stockItemUuid");
		if (StringUtils.isNotBlank(param)) {
			StockItem stockItem = Context.getService(StockItemService.class).getStockItemByUuid(param);
			if (stockItem == null) {
				return emptyResult(context);
			}
			filter.setStockItemId(stockItem.getId());
		}
		
		param = context.getParameter("excludeExpired");
		if (StringUtils.isNotBlank(param)) {
			boolean excludeExpired = param.equalsIgnoreCase("true") || param.equalsIgnoreCase("1");
			filter.setExcludeExpired(excludeExpired);
		}
		
		param = context.getParameter("locationUuid");
		if (StringUtils.isNotBlank(param)) {
			filter.setLocationUuid(param);
		}
		
		param = context.getParameter("excludeEmptyStock");
		if (StringUtils.isNotBlank(param)) {
			boolean excludeEmptyStock = param.equalsIgnoreCase("true") || param.equalsIgnoreCase("1");
			filter.setExcludeEmptyStock(excludeEmptyStock && StringUtils.isNotBlank(filter.getLocationUuid()));
		}
		
		filter.setIncludeVoided(context.getIncludeAll());
		Result<StockBatchDTO> result = new StockItemRestMapper().findStockBatches(filter);
		return toAlreadyPaged(result, context);
	}
	
	@Override
	protected PageableResult doGetAll(RequestContext context) throws ResponseException {
		return doSearch(context);
	}
	
	@Override
	public StockBatch newDelegate() {
		return new StockBatch();
	}
	
	@Override
	public StockBatch save(StockBatch delegate) {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	public void purge(StockBatch delegate, RequestContext context) throws ResponseException {
		delete(delegate, null, context);
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			description.addProperty("uuid");
			description.addProperty("batchNo");
			description.addProperty("expiration");
			description.addProperty("stockItemUuid");
			description.addProperty("voided");
			description.addProperty("expiryNotificationDate");
		}
		
		if (rep instanceof DefaultRepresentation) {
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		}
		
		if (rep instanceof FullRepresentation) {
			description.addSelfLink();
		}
		
		if (rep instanceof RefRepresentation) {
			description.addProperty("uuid");
			description.addProperty("batchNo");
			
		}
		
		return description;
	}
	
	@Override
	public Model getGETModel(Representation rep) {
		ModelImpl modelImpl = (ModelImpl) super.getGETModel(rep);
		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			modelImpl.property("uuid", new StringProperty()).property("batchNo", new StringProperty())
			        .property("expiration", new DateTimeProperty())
			        .property("expiryNotificationDate", new DateTimeProperty())
			        .property("stockItemUuid", new StringProperty()).property("voided", new BooleanProperty());
		}
		if (rep instanceof DefaultRepresentation) {}
		
		if (rep instanceof FullRepresentation) {}
		
		if (rep instanceof RefRepresentation) {
			modelImpl.property("uuid", new StringProperty()).property("batchNo", new StringProperty());
		}
		
		return modelImpl;
	}
	
}
