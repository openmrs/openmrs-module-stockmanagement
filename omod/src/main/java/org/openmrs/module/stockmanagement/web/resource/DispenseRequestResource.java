package org.openmrs.module.stockmanagement.web.resource;

import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.StringProperty;
import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.ModuleConstants;
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.stockmanagement.api.dto.DispenseRequest;
import org.openmrs.module.stockmanagement.api.dto.StockItemSearchFilter;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.*;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.module.webservices.validation.ValidateUtil;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.*;

@Resource(name = RestConstants.VERSION_1 + "/" + ModuleConstants.MODULE_ID + "/dispenserequest", supportedClass = DispenseRequest.class, supportedOpenmrsVersions = {
        "1.9.*", "1.10.*", "1.11.*", "1.12.*", "2.*" })
public class DispenseRequestResource extends ResourceBase<DispenseRequest> {
	
	@Override
	public DispenseRequest getByUniqueId(String uniqueId) {
		StockItemSearchFilter filter = new StockItemSearchFilter();
		filter.setIncludeVoided(true);
		filter.setUuid(uniqueId);
		
		return null;
	}
	
	@Override
	protected void delete(DispenseRequest delegate, String reason, RequestContext context) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	protected PageableResult doSearch(RequestContext context) {
		String searchToken = context.getParameter("q");
		if (StringUtils.isBlank(searchToken)) {
			return null;
		} else {
			return null;
		}
	}
	
	@Override
    public Object create(SimpleObject propertiesToCreate, RequestContext context) throws ResponseException {
        List<DispenseRequest> dispenseRequests = new ArrayList<>();
        SimpleObject dispenseRequestObject = new SimpleObject();
        String DISPENSE_CUSTOM_REP = "(locationUuid,patientId,orderId,encounterId,stockItemUuid,quantity,stockItemPackagingUOMUuid)";

        if (propertiesToCreate.containsKey("dispenseItems") && !propertiesToCreate.get("dispenseItems").equals(null) && !propertiesToCreate.get("dispenseItems").equals("")) {
            List<SimpleObject> dispenseItems = propertiesToCreate.get("dispenseItems");
            for (int i = 0; i < dispenseItems.size(); i++) {
                dispenseRequests.add(covertToDispenseRequest(dispenseItems.get(i)));
            }

        } else {
            dispenseRequests.add(covertToDispenseRequest(propertiesToCreate));
        }

        getStockManagementService().dispenseStockItems(dispenseRequests);

        return new NeedsPaging<>(dispenseRequests, context);
    }
	
	public DispenseRequest covertToDispenseRequest(Map dispenseItem) {
		Integer dispenseItemQuantity;
		if (!dispenseItem.get("quantity").equals("") && !dispenseItem.get("quantity").equals(null)) {
			dispenseItemQuantity = Integer.parseInt(dispenseItem.get("quantity").toString());
		} else {
			throw new IllegalArgumentException("quantity can not be null");
		}
		
		DispenseRequest dispenseRequest = new DispenseRequest();
		dispenseRequest.setLocationUuid(dispenseItem.get("locationUuid").toString());
		dispenseRequest.setPatientId(Integer.parseInt(dispenseItem.get("patientId").toString()));
		dispenseRequest.setOrderId(Integer.parseInt(dispenseItem.get("orderId").toString()));
		dispenseRequest.setEncounterId(Integer.parseInt(dispenseItem.get("encounterId").toString()));
		
		dispenseRequest.setStockItemUuid(dispenseItem.get("stockItemUuid").toString());
		dispenseRequest.setStockBatchUuid(dispenseItem.get("stockBatchUuid").toString());
		dispenseRequest.setQuantity(BigDecimal.valueOf(dispenseItemQuantity));
		dispenseRequest.setStockItemPackagingUOMUuid(dispenseItem.get("stockItemPackagingUOMUuid").toString());
		return dispenseRequest;
	}
	
	@Override
	protected PageableResult doGetAll(RequestContext context) throws ResponseException {
		return doSearch(context);
	}
	
	@Override
	public DispenseRequest newDelegate() {
		return new DispenseRequest();
	}
	
	@Override
	public DispenseRequest save(DispenseRequest delegate) {
		return null;
	}
	
	@Override
	public void purge(DispenseRequest delegate, RequestContext context) throws ResponseException {
		delete(delegate, null, context);
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("locationUuid");
		description.addProperty("patientId");
		description.addProperty("orderId");
		description.addProperty("encounterId");
		description.addProperty("stockItemUuid");
		description.addProperty("stockBatchUuid");
		description.addProperty("quantity");
		description.addProperty("stockItemPackagingUOMUuid");
		description.addProperty("dispenseItems");
		return description;
	}
	
	@Override
	public DelegatingResourceDescription getUpdatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("locationUuid");
		description.addProperty("patientId");
		description.addProperty("orderId");
		description.addProperty("encounterId");
		description.addProperty("stockItemUuid");
		description.addProperty("stockBatchUuid");
		description.addProperty("quantity");
		return description;
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			description.addProperty("locationUuid");
			description.addProperty("patientId");
			description.addProperty("orderId");
			description.addProperty("encounterId");
			description.addProperty("stockItemUuid");
			description.addProperty("stockBatchUuid");
			description.addProperty("quantity");
		}
		
		if (rep instanceof DefaultRepresentation) {
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		}
		
		if (rep instanceof FullRepresentation) {
			description.addProperty("locationUuid");
			description.addProperty("patientId");
			description.addProperty("orderId");
			description.addProperty("encounterId");
			description.addProperty("stockItemUuid");
			description.addProperty("stockBatchUuid");
			description.addProperty("quantity");
			description.addSelfLink();
		}
		
		if (rep instanceof RefRepresentation) {
			description.addProperty("locationUuid");
			description.addProperty("patientId");
			description.addProperty("orderId");
			description.addProperty("encounterId");
			description.addProperty("stockItemUuid");
			description.addProperty("stockBatchUuid");
			description.addProperty("quantity");
		}
		
		return description;
	}
	
	@Override
	public Model getGETModel(Representation rep) {
		ModelImpl modelImpl = (ModelImpl) super.getGETModel(rep);
		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			modelImpl.property("locationUuid", new StringProperty()).property("patientId", new StringProperty())
			        .property("orderId", new StringProperty()).property("encounterId", new StringProperty())
			        .property("stockItemUuid", new StringProperty()).property("stockBatchUuid", new StringProperty())
			        .property("quantity", new StringProperty());
		}
		if (rep instanceof DefaultRepresentation) {}
		
		if (rep instanceof FullRepresentation) {}
		
		if (rep instanceof RefRepresentation) {
			modelImpl.property("locationUuid", new StringProperty()).property("patientId", new StringProperty())
			        .property("orderId", new StringProperty()).property("encounterId", new StringProperty())
			        .property("stockItemUuid", new StringProperty()).property("stockBatchUuid", new StringProperty())
			        .property("quantity", new StringProperty());
		}
		
		return modelImpl;
	}
	
}
