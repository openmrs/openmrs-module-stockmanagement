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
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.DateTimeProperty;
import io.swagger.models.properties.DecimalProperty;
import io.swagger.models.properties.LongProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.APIException;
import org.openmrs.api.ValidationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.ModuleConstants;
import org.openmrs.module.stockmanagement.api.Privileges;
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.Result;
import org.openmrs.module.stockmanagement.api.dto.StockRuleSearchFilter;
import org.openmrs.module.stockmanagement.api.model.StockRule;
import org.openmrs.module.stockmanagement.api.validator.StockRuleValidator;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DataDelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.GenericRestException;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Resource(name = RestConstants.VERSION_1 + "/" + ModuleConstants.MODULE_ID
        + "/stockrule", supportedClass = StockRule.class, supportedOpenmrsVersions = { "1.9.*", "1.10.*", "1.11.*", "1.12.*",
                "2.*" })
public class StockRuleResource extends DataDelegatingCrudResource<StockRule> {
	
	private static final Log log = LogFactory.getLog(StockRuleResource.class);
	
	private StockManagementService stockManagementService;
	
	protected StockManagementService getStockManagementService() {
		if (stockManagementService == null) {
			stockManagementService = Context.getService(StockManagementService.class);
		}
		return stockManagementService;
	}
	
	protected void setStockManagementService(StockManagementService stockManagementService) {
		this.stockManagementService = stockManagementService;
	}
	
	@Override
	public StockRule getByUniqueId(String uniqueId) {
		if (StringUtils.isBlank(uniqueId)) {
			return null;
		}
		return getStockManagementService().getStockRuleByUuid(uniqueId);
	}
	
	@Override
	protected void delete(StockRule delegate, String reason, RequestContext context) throws ResponseException {
		if (reason != null && reason.length() > 250) {
			throw new IllegalRequestException("Parameter reason can not exceed 250 characters");
		}
		List<String> stockRulesToDelete = new ArrayList<>();
		stockRulesToDelete.add(delegate.getUuid());
		String ids = context.getParameter("ids");
		if (StringUtils.isNotEmpty(ids)) {
			for (String id : ids.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				if (id.length() > 38) {
					throw new IllegalRequestException("Id not recognized");
				}
				stockRulesToDelete.add(id);
			}
		}
		try {
			getStockManagementService().voidStockRules(stockRulesToDelete, reason, Context.getAuthenticatedUser().getId());
		}
		catch (ResponseException | APIAuthenticationException e) {
			throw e;
		}
		catch (StockManagementException | APIException e) {
			log.error("Failed to void stock rule. uuid=" + delegate.getUuid(), e);
			throw new IllegalRequestException(ResourceBase.extractSafeMessage(e, "Unable to void stock rule"));
		}
		catch (Exception e) {
			log.error("Failed to void stock rule. uuid=" + delegate.getUuid(), e);
			throw new GenericRestException("Unable to void stock rule");
		}
	}
	
	@Override
	protected StockRule undelete(StockRule delegate, RequestContext context) throws ResponseException {
		if (Boolean.TRUE.equals(delegate.getVoided())) {
			delegate.setVoided(false);
			delegate.setVoidedBy(null);
			delegate.setDateVoided(null);
			delegate.setVoidReason(null);
			return save(delegate);
		}
		return delegate;
	}
	
	@Override
	protected PageableResult doSearch(RequestContext context) {
		StockRuleSearchFilter filter = new StockRuleSearchFilter();
		
		String locationParam = context.getParameter("locationUuid");
		if (StringUtils.isBlank(locationParam)) {
			locationParam = context.getParameter("location");
		}
		if (StringUtils.isNotBlank(locationParam)) {
			filter.setLocationUuids(Collections.singletonList(locationParam));
		}
		
		String stockItemParam = context.getParameter("stockItemUuid");
		if (StringUtils.isBlank(stockItemParam)) {
			stockItemParam = context.getParameter("stockItem");
		}
		if (StringUtils.isNotBlank(stockItemParam)) {
			filter.setStockItemUuids(Collections.singletonList(stockItemParam));
		}
		
		String enabledParam = context.getParameter("enabled");
		if (StringUtils.isNotBlank(enabledParam)) {
			if ("true".equalsIgnoreCase(enabledParam)) {
				filter.setEnabled(Boolean.TRUE);
			} else if ("false".equalsIgnoreCase(enabledParam)) {
				filter.setEnabled(Boolean.FALSE);
			} else {
				throw new IllegalRequestException("Parameter enabled must be true or false");
			}
		}
		
		String qParam = context.getParameter("q");
		if (StringUtils.isNotBlank(qParam)) {
			filter.setName(qParam);
		}
		
		int startIndex = context.getStartIndex() != null ? context.getStartIndex() : 0;
		if (startIndex < 0) {
			throw new IllegalRequestException("Parameter startIndex cannot be negative");
		}
		int limit = context.getLimit() != null ? context.getLimit() : RestUtil.getDefaultLimit();
		
		filter.setIncludeVoided(Boolean.TRUE.equals(context.getIncludeAll()));
		filter.setStartIndex(startIndex);
		filter.setLimit(limit);
		
		try {
			Result<StockRule> result = getStockManagementService().findStockRules(filter);
			long totalCount = result.getTotalRecordCount() != null ? result.getTotalRecordCount() : result.getData().size();
			boolean hasMore = (startIndex + (long) result.getData().size()) < totalCount;
			return new AlreadyPaged<>(context, result.getData(), hasMore, totalCount);
		}
		catch (ResponseException | APIAuthenticationException e) {
			throw e;
		}
		catch (StockManagementException | APIException e) {
			log.error("Failed to search stock rules", e);
			throw new IllegalRequestException(ResourceBase.extractSafeMessage(e, "Unable to search stock rules"));
		}
		catch (Exception e) {
			log.error("Failed to search stock rules", e);
			throw new GenericRestException("Unable to search stock rules");
		}
	}
	
	@Override
	protected PageableResult doGetAll(RequestContext context) throws ResponseException {
		return doSearch(context);
	}
	
	@Override
	public StockRule newDelegate() {
		return new StockRule();
	}
	
	@Override
	public StockRule save(StockRule delegate) {
		try {
			// webservices.rest 3.2+ no longer runs the @Handler validator on REST saves
			ResourceBase.validate(new StockRuleValidator(), delegate, "stockRule");
			return getStockManagementService().saveStockRule(delegate);
		}
		catch (ResponseException | APIAuthenticationException | ValidationException e) {
			throw e;
		}
		catch (StockManagementException | APIException e) {
			log.error("Failed to save stock rule. uuid=" + (delegate != null ? delegate.getUuid() : null), e);
			throw new IllegalRequestException(ResourceBase.extractSafeMessage(e, "Unable to save stock rule"));
		}
		catch (Exception e) {
			log.error("Failed to save stock rule. uuid=" + (delegate != null ? delegate.getUuid() : null), e);
			throw new GenericRestException("Unable to save stock rule");
		}
	}
	
	@Override
	public void purge(StockRule delegate, RequestContext context) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("stockItem");
		description.addProperty("name");
		description.addProperty("description");
		description.addProperty("location");
		description.addProperty("quantity");
		description.addProperty("stockItemPackagingUOM");
		description.addProperty("enabled");
		description.addProperty("evaluationFrequency");
		description.addProperty("actionFrequency");
		description.addProperty("alertRole");
		description.addProperty("mailRole");
		description.addProperty("enableDescendants");
		return description;
	}
	
	@Override
	public DelegatingResourceDescription getUpdatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("name");
		description.addProperty("description");
		description.addProperty("quantity");
		description.addProperty("stockItemPackagingUOM");
		description.addProperty("enabled");
		description.addProperty("evaluationFrequency");
		description.addProperty("actionFrequency");
		description.addProperty("alertRole");
		description.addProperty("mailRole");
		description.addProperty("enableDescendants");
		return description;
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		if (rep instanceof DefaultRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("display");
			description.addProperty("name");
			description.addProperty("description");
			description.addProperty("stockItem", Representation.REF);
			description.addProperty("location", Representation.REF);
			description.addProperty("stockItemPackagingUOM", Representation.REF);
			description.addProperty("quantity");
			description.addProperty("enabled");
			description.addProperty("evaluationFrequency");
			description.addProperty("lastEvaluation");
			description.addProperty("nextEvaluation");
			description.addProperty("actionFrequency");
			description.addProperty("lastActionDate");
			description.addProperty("nextActionDate");
			description.addProperty("alertRole");
			description.addProperty("mailRole");
			description.addProperty("enableDescendants");
			description.addProperty("voided");
			description.addSelfLink();
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
			return description;
		}
		if (rep instanceof FullRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("display");
			description.addProperty("name");
			description.addProperty("description");
			description.addProperty("stockItem", Representation.DEFAULT);
			description.addProperty("location", Representation.DEFAULT);
			description.addProperty("stockItemPackagingUOM", Representation.DEFAULT);
			description.addProperty("quantity");
			description.addProperty("enabled");
			description.addProperty("evaluationFrequency");
			description.addProperty("lastEvaluation");
			description.addProperty("nextEvaluation");
			description.addProperty("actionFrequency");
			description.addProperty("lastActionDate");
			description.addProperty("nextActionDate");
			description.addProperty("alertRole");
			description.addProperty("mailRole");
			description.addProperty("enableDescendants");
			description.addProperty("voided");
			description.addProperty("permission");
			description.addProperty("auditInfo");
			description.addSelfLink();
			return description;
		}
		return null;
	}
	
	@PropertyGetter("display")
	public String getDisplayString(StockRule instance) {
		return instance.getName();
	}
	
	@PropertySetter("quantity")
	public void setQuantity(StockRule instance, Double value) {
		if (value == null) {
			instance.setQuantity(null);
		} else {
			instance.setQuantity(BigDecimal.valueOf(value));
		}
	}
	
	@PropertySetter("evaluationFrequency")
	public void setEvaluationFrequency(StockRule instance, Double value) {
		if (value == null) {
			instance.setEvaluationFrequency(null);
		} else {
			instance.setEvaluationFrequency(Long.valueOf(value.longValue()));
		}
	}
	
	@PropertySetter("actionFrequency")
	public void setActionFrequency(StockRule instance, Double value) {
		if (value == null) {
			instance.setActionFrequency(null);
		} else {
			instance.setActionFrequency(Long.valueOf(value.longValue()));
		}
	}
	
	@PropertyGetter("permission")
	public SimpleObject getPermission(StockRule stockRule) {
		SimpleObject simpleObject = new SimpleObject();
		simpleObject.add("canView", true);
		User user = Context.getAuthenticatedUser();
		boolean canEdit = !Boolean.TRUE.equals(stockRule.getVoided()) && user != null && stockRule.getLocation() != null
		        && getStockManagementService().userHasStockManagementPrivilege(user, stockRule.getLocation(), null,
		            Privileges.TASK_STOCKMANAGEMENT_STOCKITEMS_MUTATE);
		simpleObject.add("canEdit", canEdit);
		return simpleObject;
	}
	
	@Override
	public Model getGETModel(Representation rep) {
		ModelImpl modelImpl = (ModelImpl) super.getGETModel(rep);
		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			modelImpl.property("uuid", new StringProperty()).property("display", new StringProperty())
			        .property("name", new StringProperty()).property("description", new StringProperty())
			        .property("quantity", new DecimalProperty()).property("enabled", new BooleanProperty())
			        .property("evaluationFrequency", new LongProperty()).property("lastEvaluation", new DateTimeProperty())
			        .property("nextEvaluation", new DateTimeProperty()).property("actionFrequency", new LongProperty())
			        .property("lastActionDate", new DateTimeProperty()).property("nextActionDate", new DateTimeProperty())
			        .property("alertRole", new StringProperty()).property("mailRole", new StringProperty())
			        .property("enableDescendants", new BooleanProperty()).property("voided", new BooleanProperty());
		}
		if (rep instanceof DefaultRepresentation) {
			modelImpl.property("stockItem", new RefProperty("#/definitions/StockitemGetRef"))
			        .property("location", new RefProperty("#/definitions/LocationGetRef"))
			        .property("stockItemPackagingUOM", new RefProperty("#/definitions/StockitempackaginguomGetRef"));
		}
		if (rep instanceof FullRepresentation) {
			modelImpl.property("stockItem", new RefProperty("#/definitions/StockitemGet"))
			        .property("location", new RefProperty("#/definitions/LocationGet"))
			        .property("stockItemPackagingUOM", new RefProperty("#/definitions/StockitempackaginguomGet"))
			        .property("permission", new ObjectProperty());
		}
		if (rep instanceof RefRepresentation) {
			modelImpl.property("uuid", new StringProperty()).property("display", new StringProperty()).property("voided",
			    new BooleanProperty());
		}
		return modelImpl;
	}
	
	@Override
	public Model getCREATEModel(Representation rep) {
		return new ModelImpl().property("stockItem", new StringProperty().example("uuid"))
		        .property("name", new StringProperty()).property("description", new StringProperty())
		        .property("location", new StringProperty().example("uuid")).property("quantity", new DecimalProperty())
		        .property("stockItemPackagingUOM", new StringProperty().example("uuid"))
		        .property("enabled", new BooleanProperty()).property("evaluationFrequency", new LongProperty())
		        .property("actionFrequency", new LongProperty()).property("alertRole", new StringProperty())
		        .property("mailRole", new StringProperty()).property("enableDescendants", new BooleanProperty());
	}
	
	@Override
	public Model getUPDATEModel(Representation rep) {
		return new ModelImpl().property("name", new StringProperty()).property("description", new StringProperty())
		        .property("quantity", new DecimalProperty())
		        .property("stockItemPackagingUOM", new StringProperty().example("uuid"))
		        .property("enabled", new BooleanProperty()).property("evaluationFrequency", new LongProperty())
		        .property("actionFrequency", new LongProperty()).property("alertRole", new StringProperty())
		        .property("mailRole", new StringProperty()).property("enableDescendants", new BooleanProperty())
		        .property("voided", new BooleanProperty());
	}
}
