package org.openmrs.module.stockmanagement.web.resource;

import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.*;
import io.swagger.models.properties.StringProperty;
import org.apache.commons.lang.StringUtils;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import io.swagger.models.properties.*;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.appframework.service.AppFrameworkService;
import org.openmrs.module.stockmanagement.api.ModuleConstants;
import org.openmrs.module.stockmanagement.api.Privileges;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.*;
import org.openmrs.module.stockmanagement.api.model.*;
import org.openmrs.module.stockmanagement.api.reporting.Report;
import org.openmrs.module.stockmanagement.api.reporting.ReportGenerator;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Resource(name = RestConstants.VERSION_1 + "/" + ModuleConstants.MODULE_ID + "/report", supportedClass = Report.class, supportedOpenmrsVersions = {
        "1.9.*", "1.10.*", "1.11.*", "1.12.*", "2.*" })
public class ReportResource extends ResourceBase<Report> {
	
	@Override
    public Report getByUniqueId(String uniqueId) {
         Optional<Report> report = Report.getAllReports().stream().filter(p-> p.getUuid().equals(uniqueId)).findAny();
        return report.isPresent() ? report.get() : null;
    }
	
	@Override
	protected void delete(Report delegate, String reason, RequestContext context) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	protected PageableResult doSearch(RequestContext context) {
		return doGetAll(context);
	}
	
	@Override
	protected PageableResult doGetAll(RequestContext context) throws ResponseException {
		return toAlreadyPaged(getStockManagementService().getReports(), context);
	}
	
	@Override
	public Report newDelegate() {
		return new Report<ReportGenerator>();
	}
	
	@Override
	public Report save(Report delegate) {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	public void purge(Report delegate, RequestContext context) throws ResponseException {
		delete(delegate, null, context);
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			description.addProperty("order");
			description.addProperty("name");
			description.addProperty("uuid");
			description.addProperty("parameters");
			description.addProperty("systemName");
		}
		
		if (rep instanceof DefaultRepresentation) {
			
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		}
		
		if (rep instanceof FullRepresentation) {
			
			description.addSelfLink();
		}
		
		if (rep instanceof RefRepresentation) {
			
		}
		
		return description;
	}
	
	@Override
	public Model getGETModel(Representation rep) {
		ModelImpl modelImpl = (ModelImpl) super.getGETModel(rep);
		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			modelImpl.property("order", new IntegerProperty()).property("name", new StringProperty())
			        .property("systemName", new StringProperty()).property("parameters", new ArrayProperty());
		}
		if (rep instanceof DefaultRepresentation) {
			
		}
		
		if (rep instanceof FullRepresentation) {
			
		}
		
		if (rep instanceof RefRepresentation) {
			
		}
		
		return modelImpl;
	}
	
}
