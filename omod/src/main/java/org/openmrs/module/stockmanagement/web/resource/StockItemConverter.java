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
import org.openmrs.module.stockmanagement.api.model.StockItem;
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

@Handler(supports = { StockItem.class }, order = 50)
public class StockItemConverter extends BaseDelegatingConverter<StockItem> {
	
	@Override
	public StockItem getByUniqueId(String uniqueId) {
		if (StringUtils.isBlank(uniqueId)) {
			return null;
		}
		return Context.getService(StockManagementService.class).getStockItemByUuid(uniqueId);
	}
	
	@Override
	public StockItem newInstance(String type) {
		return new StockItem();
	}
	
	@Override
	public String getUri(Object instance) {
		if (!(instance instanceof StockItem) || ((StockItem) instance).getUuid() == null) {
			return "";
		}
		return RestConstants.URI_PREFIX + RestConstants.VERSION_1 + "/" + ModuleConstants.MODULE_ID + "/stockitem/"
		        + ((StockItem) instance).getUuid();
	}
	
	@PropertyGetter("display")
	public String getDisplayString(StockItem instance) {
		if (StringUtils.isNotBlank(instance.getCommonName())) {
			return instance.getCommonName();
		}
		if (instance.getDrug() != null && StringUtils.isNotBlank(instance.getDrug().getName())) {
			return instance.getDrug().getName();
		}
		if (instance.getConcept() != null) {
			return instance.getConcept().getDisplayString();
		}
		return instance.getUuid();
	}
	
	@PropertyGetter("auditInfo")
	public SimpleObject getAuditInfo(StockItem instance) {
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
			description.addProperty("drug", Representation.REF);
			description.addProperty("concept", Representation.REF);
			description.addProperty("commonName");
			description.addProperty("acronym");
			description.addProperty("hasExpiration");
			description.addProperty("isDrug");
			description.addProperty("voided");
			description.addSelfLink();
			return description;
		}
		if (rep instanceof FullRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("display");
			description.addProperty("drug", Representation.DEFAULT);
			description.addProperty("concept", Representation.DEFAULT);
			description.addProperty("commonName");
			description.addProperty("acronym");
			description.addProperty("hasExpiration");
			description.addProperty("isDrug");
			description.addProperty("preferredVendor", Representation.REF);
			description.addProperty("purchasePrice");
			description.addProperty("purchasePriceUoM", Representation.REF);
			description.addProperty("dispensingUnit", Representation.REF);
			description.addProperty("dispensingUnitPackagingUoM", Representation.REF);
			description.addProperty("defaultStockOperationsUoM", Representation.REF);
			description.addProperty("reorderLevel");
			description.addProperty("reorderLevelUOM", Representation.REF);
			description.addProperty("category", Representation.REF);
			description.addProperty("expiryNotice");
			description.addProperty("voided");
			description.addProperty("auditInfo");
			description.addSelfLink();
			return description;
		}
		return null;
	}
}
