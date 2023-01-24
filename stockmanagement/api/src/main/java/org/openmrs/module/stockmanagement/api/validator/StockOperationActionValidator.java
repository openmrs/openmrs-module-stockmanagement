package org.openmrs.module.stockmanagement.api.validator;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.dto.*;
import org.openmrs.module.stockmanagement.api.model.*;
import org.openmrs.module.stockmanagement.api.utils.DateUtil;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Handler(supports = { StockOperationDTO.class }, order = 50)
public class StockOperationActionValidator implements Validator {
	
	@Override
	public boolean supports(Class<?> aClass) {
		return StockOperationAction.class.isAssignableFrom(aClass);
	}
	
	@Override
	public void validate(Object target, Errors errors) {
		MessageSourceService messageSourceService = Context.getMessageSourceService();
		if (target == null) {
			errors.reject(messageSourceService.getMessage("error.general"));
			return;
		}
		
		if (Context.getAuthenticatedUser() == null) {
			errors.reject(messageSourceService.getMessage("stockmanagement.stockoperation.authrequired"));
			return;
		}
		
		StockOperationAction object = (StockOperationAction) target;
		if (object.getName() == null) {
			errors.rejectValue("name", messageSourceService.getMessage("stockmanagement.stockoperation.action.namerequired"));
			return;
		}
		
		if (object.getUuid() == null) {
			errors.rejectValue("uuid", messageSourceService.getMessage("stockmanagement.stockoperation.action.namerequired"));
			return;
		}
		
		if (object.getName().equals(StockOperationAction.Action.REJECT)
		        || object.getName().equals(StockOperationAction.Action.RETURN)
		        || object.getName().equals(StockOperationAction.Action.CANCEL)) {
			if (StringUtils.isBlank(object.getReason())) {
				errors.rejectValue("reason",
				    messageSourceService.getMessage("stockmanagement.stockoperation.action.reasonrequired"));
				return;
			}
			
			if (object.getReason().length() > 500) {
				errors.rejectValue("reason",
				    messageSourceService.getMessage("stockmanagement.stockoperation.action.reasonexceeds500"));
				return;
			}
		}
	}
}
