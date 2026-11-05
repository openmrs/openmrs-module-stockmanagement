package org.openmrs.module.stockmanagement.web.resource;

import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.StringProperty;
import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.ModuleConstants;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.StockItemPackagingUomReferenceDTO;
import org.openmrs.module.stockmanagement.api.model.StockItemPackagingUomReference;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.util.ArrayList;
import java.util.List;

@Resource(name = RestConstants.VERSION_1 + "/" + ModuleConstants.MODULE_ID + "/stockitempackaginguomreference", supportedClass = StockItemPackagingUomReferenceDTO.class, supportedOpenmrsVersions = {
        "1.9.*", "1.10.*", "1.11.*", "1.12.*", "2.*" })
public class StockItemPackagingUomReferenceResource extends ResourceBase<StockItemPackagingUomReferenceDTO> {

	@Override
	public StockItemPackagingUomReferenceDTO getByUniqueId(String uniqueId) {
		return convertToDTO(getStockManagementService().getStockItemPackagingUomReferenceByUuid(uniqueId));
	}

	@Override
	protected void delete(StockItemPackagingUomReferenceDTO delegate, String reason, RequestContext context)
	        throws ResponseException {
		getStockManagementService().voidStockItemPackagingUomReference(delegate.getUuid(), reason,
		    Context.getAuthenticatedUser().getUserId());
	}

	@Override
	protected PageableResult doSearch(RequestContext context) {
		String stockItemPackagingUomUuid = context.getParameter("stockItemPackagingUomUuid");
		if (StringUtils.isNotBlank(stockItemPackagingUomUuid)) {
			List<StockItemPackagingUomReferenceDTO> referenceDTOS = new ArrayList<>();
			// an unknown packaging UOM uuid yields an empty result rather than a not-found error
			for (StockItemPackagingUomReference reference : getStockManagementService()
			        .getStockItemPackagingUomReferencesByPackagingUom(stockItemPackagingUomUuid)) {
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

		invalidRequest("stockmanagement.stockitempackaginguomreference.search.parameter.required");
		return null;
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
		StockItemPackagingUomReference reference = getStockManagementService()
		        .saveStockItemPackagingUomReference(convertFromDTO(delegate));
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
		description.addProperty("uuid");
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

	@PropertySetter("referenceCode")
	public void setReferenceCode(StockItemPackagingUomReferenceDTO instance, String value) {
		if (value == null) {
			instance.setReferenceCode(null);
		} else {
			instance.setReferenceCode(value);
		}
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
			StockItemPackagingUomReference reference = stockManagementService
			        .getStockItemPackagingUomReferenceByUuid(dto.getUuid());
			if (reference == null) {
				reference = new StockItemPackagingUomReference();
				reference.setStockItemPackagingUOM(
				    stockManagementService.getStockItemPackagingUOMByUuid(dto.getStockItemPackagingUomUuid()));
				reference.setReferenceSource(stockManagementService.getStockSourceByUuid(dto.getStockSourceUuid()));
			} else {
				// Packaging association and source namespace are not reassignable on update
				if (dto.getStockItemPackagingUomUuid() != null
				        && reference.getStockItemPackagingUOM().getUuid() != null
				        && !dto.getStockItemPackagingUomUuid().equals(reference.getStockItemPackagingUOM().getUuid())) {
					invalidRequest("stockmanagement.stockitempackaginguomreference.packaginguom.reassign");
				}
				if (dto.getStockSourceUuid() != null && reference.getReferenceSource().getUuid() != null
				        && !dto.getStockSourceUuid().equals(reference.getReferenceSource().getUuid())) {
					invalidRequest("stockmanagement.stockitempackaginguomreference.source.reassign");
				}
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
