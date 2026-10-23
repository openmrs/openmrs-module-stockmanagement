package org.openmrs.module.stockmanagement.web.resource;

import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.Result;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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
		throw new RestClientException("403: Forbidden");
	}
	
	protected void notFound() {
		throw new RestClientException("404: Forbidden");
	}
	
	protected void invalidRequest(String messageKey, Object... args) {
		if (args != null && args.length > 0)
			throw new RestClientException(String.format(Context.getMessageSourceService().getMessage(messageKey), args));
		else
			throw new RestClientException(Context.getMessageSourceService().getMessage(messageKey));
	}
	
	/**
	 * Runs a DTO validator explicitly: webservices.rest 3.x removed the ValidateUtil.validate call
	 * from DelegatingCrudResource.create, so the @Handler validators no longer run on REST saves.
	 * Throws IllegalRequestException carrying the validator's messages when it rejects the
	 * delegate.
	 */
	protected void validateDelegate(Validator validator, Object delegate, String objectName) {
		Errors errors = new MapBindingResult(new HashMap<>(), objectName);
		validator.validate(delegate, errors);
		if (errors.hasErrors()) {
			// the module's validators pass the human-readable text as the error code, so fall back
			// to it when no default message was resolved
			String message = errors.getAllErrors().stream()
			        .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : error.getCode())
			        .map(message1 -> message1 == null ? "Invalid request" : message1)
			        .collect(Collectors.joining(", "));
			throw new IllegalRequestException(message);
		}
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
	