package org.openmrs.module.stockmanagement.web.resource;

import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.appframework.service.AppFrameworkService;
import org.openmrs.module.stockmanagement.api.ModuleConstants;
import org.openmrs.module.stockmanagement.api.dto.DashboardExtensionDTO;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.util.List;
import java.util.stream.Collectors;

@Resource(name = RestConstants.VERSION_1 + "/" + ModuleConstants.MODULE_ID + "/dashboardextension", supportedClass = DashboardExtensionDTO.class, supportedOpenmrsVersions = {
        "1.9.*", "1.10.*", "1.11.*", "1.12.*", "2.*" })
public class DashboardExtensionResource extends DelegatingCrudResource<DashboardExtensionDTO> {
	
	@Override
	public DashboardExtensionDTO getByUniqueId(String uniqueId) {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	protected void delete(DashboardExtensionDTO delegate, String reason, RequestContext context) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	protected PageableResult doSearch(RequestContext context) {
		return doGetAll(context);
	}
	
	@Override
    protected PageableResult doGetAll(RequestContext context) throws ResponseException {
        AppFrameworkService appFrameworkService = Context.getService(AppFrameworkService.class);
        MessageSourceService messageSourceService = Context.getMessageSourceService();
        List<DashboardExtensionDTO> extensions = appFrameworkService.getExtensionsForCurrentUser(ModuleConstants.DASHBOARD_EXTENSION_POINT_ID)
                .stream()
                .map(p -> {
                    DashboardExtensionDTO extension = new DashboardExtensionDTO();
                    extension.setId(p.getId());
                    extension.setIcon(p.getIcon());
                    extension.setLabel(messageSourceService.getMessage(p.getLabel()));
                    extension.setOrder(p.getOrder());
                    extension.setType(p.getType());
                    extension.setUrl(p.getUrl());
                    return extension;
                }).collect(Collectors.toList());

        return new NeedsPaging<>(extensions, context);
    }
	
	@Override
	public DashboardExtensionDTO newDelegate() {
		return new DashboardExtensionDTO();
	}
	
	@Override
	public DashboardExtensionDTO save(DashboardExtensionDTO delegate) {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	public void purge(DashboardExtensionDTO delegate, RequestContext context) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException();
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("id");
		description.addProperty("order");
		description.addProperty("label");
		description.addProperty("type");
		description.addProperty("url");
		description.addProperty("icon");
		return description;
	}
}
