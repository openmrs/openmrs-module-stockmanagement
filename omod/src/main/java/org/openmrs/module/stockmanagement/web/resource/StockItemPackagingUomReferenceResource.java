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
import io.swagger.models.properties.StringProperty;
import org.apache.commons.lang.StringUtils;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.ModuleConstants;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.StockSourceService;
import org.openmrs.module.stockmanagement.api.dto.StockItemPackagingUomReferenceDTO;
import org.openmrs.module.stockmanagement.api.model.StockItemPackagingUomReference;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.GenericRestException;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

@Resource(name = RestConstants.VERSION_1 + "/" + ModuleConstants.MODULE_ID
        + "/stockitempackaginguomreference", supportedClass = StockItemPackagingUomReferenceDTO.class, supportedOpenmrsVersions = {
                "1.9.*", "1.10.*", "1.11.*", "1.12.*", "2.*" })
public class StockItemPackagingUomReferenceResource extends ResourceBase<StockItemPackagingUomReferenceDTO> {
	
	private static final Log log = LogFactory.getLog(StockItemPackagingUomReferenceResource.class);
	
	@Override
	public StockItemPackagingUomReferenceDTO getByUniqueId(String uniqueId) {
		return convertToDTO(getStockManagementService().getStockItemPackagingUomReferenceByUuid(uniqueId));
	}
	
	@Override
	protected void delete(StockItemPackagingUomReferenceDTO delegate, String reason, RequestContext context)
	        throws ResponseException {
		// void_reason is VARCHAR(255): an over-long reason would overflow at flush and come back
		// as the generic 500, so refuse it with a 400 like the sibling resources do
		if (reason != null && reason.length() > 250) {
			throw new IllegalRequestException("Parameter reason can not exceed 250 characters");
		}
		try {
			getStockManagementService().voidStockItemPackagingUomReference(delegate.getUuid(), reason,
			    Context.getAuthenticatedUser().getUserId());
		}
		catch (APIAuthenticationException e) {
			throw e;
		}
		catch (APIException e) {
			// the REST module answers service rejections with a 500 otherwise; the cause stays
			// in the log, since wrapErrorResponse appends each cause's message to the response
			log.error("Failed to void stock item packaging UOM reference. uuid=" + delegate.getUuid(), e);
			throw new IllegalRequestException(safeMessage(e, "Unable to void stock item packaging UOM reference"));
		}
		catch (Exception e) {
			// unexpected failure: generic message to the client, original exception in the log
			log.error("Failed to void stock item packaging UOM reference. uuid=" + delegate.getUuid(), e);
			throw new GenericRestException("Unable to void stock item packaging UOM reference");
		}
	}
	
	@Override
	protected PageableResult doSearch(RequestContext context) {
		String stockItemPackagingUomUuid = context.getParameter("stockItemPackagingUomUuid");
		if (StringUtils.isNotBlank(stockItemPackagingUomUuid)) {
			List<StockItemPackagingUomReferenceDTO> referenceDTOS = new ArrayList<>();
			// an unknown packaging UOM uuid yields an empty result rather than a not-found error;
			// includeAll matches the sibling resources so voided references stay listable for audit
			for (StockItemPackagingUomReference reference : getStockManagementService()
			        .getStockItemPackagingUomReferencesByPackagingUom(stockItemPackagingUomUuid,
			            Boolean.TRUE.equals(context.getIncludeAll()))) {
				referenceDTOS.add(convertToDTO(reference));
			}
			return toAlreadyPaged(referenceDTOS, context);
		}
		
		String stockSourceUuid = context.getParameter("stockSourceUuid");
		String referenceCode = context.getParameter("referenceCode");
		if (StringUtils.isNotBlank(stockSourceUuid) && StringUtils.isNotBlank(referenceCode)) {
			List<StockItemPackagingUomReferenceDTO> referenceDTOS = new ArrayList<>();
			for (StockItemPackagingUomReference reference : getStockManagementService()
			        .getStockItemPackagingUomReferencesBySourceAndCode(stockSourceUuid, referenceCode)) {
				referenceDTOS.add(convertToDTO(reference));
			}
			return toAlreadyPaged(referenceDTOS, context);
		}
		
		// thrown as a bad request directly, since invalidRequest's RestClientException answers a 500
		throw new IllegalRequestException(Context.getMessageSourceService()
		        .getMessage("stockmanagement.stockitempackaginguomreference.search.parameter.required"));
	}
	
	@Override
	protected PageableResult doGetAll(RequestContext context) throws ResponseException {
		return doSearch(context);
	}
	
	@Override
	public StockItemPackagingUomReferenceDTO newDelegate() {
		return new StockItemPackagingUomReferenceDTO();
	}
	
	@Override
	public StockItemPackagingUomReferenceDTO save(StockItemPackagingUomReferenceDTO delegate) {
		StockItemPackagingUomReference reference;
		try {
			reference = getStockManagementService().saveStockItemPackagingUomReference(convertFromDTO(delegate));
		}
		catch (APIAuthenticationException e) {
			throw e;
		}
		catch (APIException e) {
			// the REST module answers service rejections with a 500 otherwise; the cause stays
			// in the log, since wrapErrorResponse appends each cause's message to the response
			log.error("Failed to save stock item packaging UOM reference. uuid=" + delegate.getUuid() + ", packagingUomUuid="
			        + delegate.getStockItemPackagingUomUuid() + ", sourceUuid=" + delegate.getStockSourceUuid()
			        + ", referenceCode=" + delegate.getReferenceCode(),
			    e);
			throw new IllegalRequestException(safeMessage(e, "Unable to save stock item packaging UOM reference"));
		}
		catch (Exception e) {
			// unexpected failure (e.g. the check-then-insert race lost on the unique constraint):
			// generic message to the client, original exception in the log
			log.error("Failed to save stock item packaging UOM reference. uuid=" + delegate.getUuid() + ", packagingUomUuid="
			        + delegate.getStockItemPackagingUomUuid() + ", sourceUuid=" + delegate.getStockSourceUuid()
			        + ", referenceCode=" + delegate.getReferenceCode(),
			    e);
			throw new GenericRestException("Unable to save stock item packaging UOM reference");
		}
		return getByUniqueId(reference.getUuid());
	}
	
	@Override
	public void purge(StockItemPackagingUomReferenceDTO delegate, RequestContext context) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("stockItemPackagingUomUuid");
		description.addProperty("stockSourceUuid");
		description.addProperty("referenceCode");
		description.addProperty("name");
		description.addProperty("description");
		return description;
	}
	
	@Override
	public DelegatingResourceDescription getUpdatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("referenceCode");
		description.addProperty("name");
		description.addProperty("description");
		return description;
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			description.addProperty("uuid");
			description.addProperty("referenceCode");
			description.addProperty("name");
			description.addProperty("description");
			description.addProperty("stockSourceUuid");
			description.addProperty("stockSourceName");
			description.addProperty("stockItemPackagingUomUuid");
			description.addProperty("voided");
		}
		
		if (rep instanceof DefaultRepresentation) {
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		}
		
		if (rep instanceof FullRepresentation) {
			description.addSelfLink();
		}
		
		if (rep instanceof RefRepresentation) {
			description.addProperty("uuid");
			description.addProperty("referenceCode");
			description.addProperty("stockSourceName");
			description.addProperty("stockItemPackagingUomUuid");
		}
		
		return description;
	}
	
	@Override
	public Model getGETModel(Representation rep) {
		ModelImpl modelImpl = (ModelImpl) super.getGETModel(rep);
		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			modelImpl.property("uuid", new StringProperty()).property("referenceCode", new StringProperty())
			        .property("name", new StringProperty()).property("description", new StringProperty())
			        .property("stockSourceUuid", new StringProperty()).property("stockSourceName", new StringProperty())
			        .property("stockItemPackagingUomUuid", new StringProperty());
		}
		
		if (rep instanceof RefRepresentation) {
			modelImpl.property("uuid", new StringProperty()).property("referenceCode", new StringProperty())
			        .property("stockSourceName", new StringProperty())
			        .property("stockItemPackagingUomUuid", new StringProperty());
		}
		
		return modelImpl;
	}
	
	public StockItemPackagingUomReferenceDTO convertToDTO(StockItemPackagingUomReference reference) {
		if (reference != null) {
			StockItemPackagingUomReferenceDTO referenceDTO = new StockItemPackagingUomReferenceDTO();
			referenceDTO.setId(reference.getId());
			referenceDTO.setUuid(reference.getUuid());
			referenceDTO.setVoided(reference.getVoided());
			referenceDTO.setReferenceCode(reference.getStockReferenceCode());
			referenceDTO.setName(reference.getName());
			referenceDTO.setDescription(reference.getDescription());
			referenceDTO.setStockSourceId(reference.getReferenceSource().getId());
			referenceDTO.setStockSourceUuid(reference.getReferenceSource().getUuid());
			referenceDTO.setStockSourceName(reference.getReferenceSource().getName());
			referenceDTO.setStockItemPackagingUomId(reference.getStockItemPackagingUOM().getId());
			referenceDTO.setStockItemPackagingUomUuid(reference.getStockItemPackagingUOM().getUuid());
			return referenceDTO;
		} else {
			return null;
		}
	}
	
	public StockItemPackagingUomReference convertFromDTO(StockItemPackagingUomReferenceDTO dto) {
		StockManagementService stockManagementService = Context.getService(StockManagementService.class);
		if (dto != null) {
			// only look up the stored reference on an update (a delegate loaded from storage carries
			// its id); a create carrying an existing uuid must not overwrite that reference
			StockItemPackagingUomReference reference = dto.getId() == null ? null
			        : stockManagementService.getStockItemPackagingUomReferenceByUuid(dto.getUuid());
			if (reference == null) {
				reference = new StockItemPackagingUomReference();
				reference.setStockItemPackagingUOM(
				    stockManagementService.getStockItemPackagingUOMByUuid(dto.getStockItemPackagingUomUuid()));
				reference.setReferenceSource(
				    Context.getService(StockSourceService.class).getStockSourceByUuid(dto.getStockSourceUuid()));
			}
			reference.setStockReferenceCode(dto.getReferenceCode());
			reference.setName(dto.getName());
			reference.setDescription(dto.getDescription());
			return reference;
		} else {
			return null;
		}
	}
	
}
