package org.openmrs.module.stockmanagement.web.resource;

import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.Result;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;

import java.util.ArrayList;
import java.util.List;

public abstract class ResourceBase<T> extends DelegatingCrudResource<T> {

	private StockManagementService stockManagementService;

	protected StockManagementService getStockManagementService() {
		if (stockManagementService == null) {
			stockManagementService = Context.getService(StockManagementService.class);
		}
		return stockManagementService;
	}

	/**
	 * Exposes the service for tests; production code resolves it lazily from the context.
	 */
	protected void setStockManagementService(StockManagementService stockManagementService) {
		this.stockManagementService = stockManagementService;
	}

	protected void forbidden() {
		// webservices.rest 3.0.0 (this module's compile dependency) has no ForbiddenException;
		// IllegalRequestException at least renders a clear message instead of a 500 with a raw
		// "403: Forbidden" string like the previous RestClientException did
		throw new IllegalRequestException("You are not allowed to perform this operation");
	}

	protected void notFound() {
		throw new ObjectNotFoundException("The requested resource was not found");
	}

	protected void invalidRequest(String messageKey, Object... args) {
		if (args != null && args.length > 0)
			throw new IllegalRequestException(
			        String.format(Context.getMessageSourceService().getMessage(messageKey), args));
		else
			throw new IllegalRequestException(Context.getMessageSourceService().getMessage(messageKey));
	}

	/**
	 * Returns the exception's message, falling back to the given default when it is null or blank
	 * so that REST error responses never carry a null message.
	 */
	protected String safeMessage(Throwable exception, String defaultMessage) {
		if (exception == null)
			return defaultMessage;
		String message = exception.getMessage();
		if (message == null || message.trim().isEmpty())
			return defaultMessage;
		return message;
	}
	
	protected void requirePriviledge(String priviledge) {
		UserContext userContext = Context.getUserContext();
		if (!userContext.hasPrivilege(priviledge))
			forbidden();
	}
	
	protected String nullIfEmpty(String string) {
		if (string == null)
			return null;
		string = string.trim();
		if (string.isEmpty())
			return null;
		return string;
	}
	
	protected <E> AlreadyPaged<E> emptyResult(RequestContext context) {
		return new AlreadyPaged<E>(context, new ArrayList<E>(), false);
	}
	
	protected <E> AlreadyPaged<E> toAlreadyPaged(Result<E> result, RequestContext context) {
		return new AlreadyPaged<E>(context, result.getData(), result.hasMoreResults(), result.getTotalRecordCount());
	}
	
	protected <E> AlreadyPaged<E> toAlreadyPaged(List<E> result, RequestContext context) {
		return new AlreadyPaged<E>(context, result, false, (long) result.size());
	}
	
}
