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

import org.apache.commons.lang.StringUtils;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.ModuleConstants;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.model.StockItemPackagingUOM;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.BaseDelegatingConverter;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;

@Handler(supports = { StockItemPackagingUOM.class }, order = 50)
public class StockItemPackagingUOMConverter extends BaseDelegatingConverter<StockItemPackagingUOM> {
	
	@Override
	public StockItemPackagingUOM getByUniqueId(String uniqueId) {
		if (StringUtils.isBlank(uniqueId)) {
			return null;
		}
		return Context.getService(StockManagementService.class).getStockItemPackagingUOMByUuid(uniqueId);
	}
	
	@Override
	public StockItemPackagingUOM newInstance(String type) {
		return new StockItemPackagingUOM();
	}
	
	@Override
	public String getUri(Object instance) {
		if (!(instance instanceof StockItemPackagingUOM) || ((StockItemPackagingUOM) instance).getUuid() == null) {
			return "";
		}
		return RestConstants.URI_PREFIX + RestConstants.VERSION_1 + "/" + ModuleConstants.MODULE_ID
		        + "/stockitempackaginguom/" + ((StockItemPackagingUOM) instance).getUuid();
	}
	
	@PropertyGetter("display")
	public String getDisplayString(StockItemPackagingUOM instance) {
		if (instance.getPackagingUom() != null) {
			return instance.getPackagingUom().getDisplayString();
		}
		return instance.getUuid();
	}
	
	@PropertyGetter("auditInfo")
	public SimpleObject getAuditInfo(StockItemPackagingUOM instance) {
		return ConversionUtil.getAuditInfo(instance);
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		if (rep instanceof RefRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("display");
			description.addSelfLink();
			return description;
		}
		if (rep instanceof DefaultRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("display");
			description.addProperty("factor");
			description.addProperty("packagingUom", Representation.REF);
			description.addProperty("stockItem", Representation.REF);
			description.addProperty("voided");
			description.addSelfLink();
			return description;
		}
		if (rep instanceof FullRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("display");
			description.addProperty("factor");
			description.addProperty("packagingUom", Representation.DEFAULT);
			description.addProperty("stockItem", Representation.DEFAULT);
			description.addProperty("voided");
			description.addProperty("auditInfo");
			description.addSelfLink();
			return description;
		}
		return null;
	}
}
