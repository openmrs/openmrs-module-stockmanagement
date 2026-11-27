/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.validator;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Location;
import org.openmrs.Role;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.module.stockmanagement.api.model.StockItemPackagingUOM;
import org.openmrs.module.stockmanagement.api.model.StockRule;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.math.BigDecimal;

@Handler(supports = { StockRule.class }, order = 50)
public class StockRuleValidator implements Validator {
	
	@Override
	public boolean supports(Class<?> aClass) {
		return StockRule.class.isAssignableFrom(aClass);
	}
	
	@Override
	public void validate(Object target, Errors errors) {
		MessageSourceService messageSourceService = Context.getMessageSourceService();
		if (target == null) {
			errors.reject(messageSourceService.getMessage("error.general"));
			return;
		}
		
		StockManagementService service = Context.getService(StockManagementService.class);
		StockRule object = (StockRule) target;
		StockItem stockItem = object.getStockItem();
		
		if (stockItem == null) {
			errors.rejectValue("stockItem",
			    String.format(messageSourceService.getMessage("stockmanagement.stockrule.fieldrequired"), "stock item"));
			return;
		} else if (stockItem.getId() == null) {
			stockItem = stockItem.getUuid() != null ? service.getStockItemByUuid(stockItem.getUuid()) : null;
			if (stockItem == null) {
				errors.rejectValue("stockItem", String.format(
				    messageSourceService.getMessage("stockmanagement.stockrule.fieldvaluenotexist"), "stock item"));
				return;
			}
		}
		
		if (StringUtils.isBlank(object.getName())) {
			errors.rejectValue("name",
			    String.format(messageSourceService.getMessage("stockmanagement.stockrule.fieldrequired"), "name"));
			return;
		} else if (object.getName().length() > 255) {
			errors.rejectValue("name",
			    String.format(messageSourceService.getMessage("stockmanagement.stockrule.exceedslimit"), "name", 255));
			return;
		}
		
		if (!StringUtils.isBlank(object.getDescription()) && object.getDescription().length() > 500) {
			errors.rejectValue("description", String
			        .format(messageSourceService.getMessage("stockmanagement.stockrule.exceedslimit"), "description", 500));
			return;
		}
		
		Location location = object.getLocation();
		if (location == null) {
			errors.rejectValue("location",
			    String.format(messageSourceService.getMessage("stockmanagement.stockrule.fieldrequired"), "location"));
			return;
		} else if (location.getId() == null) {
			location = location.getUuid() != null ? Context.getLocationService().getLocationByUuid(location.getUuid())
			        : null;
			if (location == null) {
				errors.rejectValue("location", String.format(
				    messageSourceService.getMessage("stockmanagement.stockrule.fieldvaluenotexist"), "location"));
				return;
			}
		}
		
		if (object.getQuantity() == null) {
			errors.rejectValue("quantity",
			    String.format(messageSourceService.getMessage("stockmanagement.stockrule.fieldrequired"), "quantity"));
			return;
		} else if (object.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
			errors.rejectValue("quantity", String.format(
			    messageSourceService.getMessage("stockmanagement.stockrule.qtygreaterthanequalzero"), "quantity"));
			return;
		}
		
		if (object.getEnabled() == null) {
			errors.rejectValue("enabled",
			    String.format(messageSourceService.getMessage("stockmanagement.stockrule.fieldrequired"), "enabled"));
			return;
		}
		
		if (object.getEvaluationFrequency() == null) {
			errors.rejectValue("evaluationFrequency", String.format(
			    messageSourceService.getMessage("stockmanagement.stockrule.fieldrequired"), "evaluation frequency"));
			return;
		} else if (object.getEvaluationFrequency().compareTo(Long.valueOf(0)) <= 0) {
			errors.rejectValue("evaluationFrequency",
			    String.format(messageSourceService.getMessage("stockmanagement.stockrule.qtygreaterthanequalzero"),
			        "evaluation frequency"));
			return;
		}
		
		if (object.getActionFrequency() == null) {
			errors.rejectValue("actionFrequency", String
			        .format(messageSourceService.getMessage("stockmanagement.stockrule.fieldrequired"), "action frequency"));
			return;
		} else if (object.getActionFrequency().compareTo(Long.valueOf(0)) <= 0) {
			errors.rejectValue("actionFrequency", String.format(
			    messageSourceService.getMessage("stockmanagement.stockrule.qtygreaterthanequalzero"), "action frequency"));
			return;
		}
		
		StockItemPackagingUOM stockItemPackagingUOM = object.getStockItemPackagingUOM();
		if (stockItemPackagingUOM == null) {
			errors.rejectValue("stockItemPackagingUOM",
			    String.format(messageSourceService.getMessage("stockmanagement.stockrule.fieldrequired"), "packaging unit"));
			return;
		} else {
			if (stockItemPackagingUOM.getId() == null) {
				stockItemPackagingUOM = stockItemPackagingUOM.getUuid() != null
				        ? service.getStockItemPackagingUOMByUuid(stockItemPackagingUOM.getUuid())
				        : null;
			}
			if (stockItemPackagingUOM == null) {
				errors.rejectValue("stockItemPackagingUOM", String.format(
				    messageSourceService.getMessage("stockmanagement.stockrule.fieldvaluenotexist"), "packaging unit"));
				return;
			} else if (!stockItemPackagingUOM.getStockItem().getUuid().equals(stockItem.getUuid())) {
				errors.rejectValue("stockItemPackagingUOM",
				    messageSourceService.getMessage("stockmanagement.stockrule.packagingunitinvalidforstockitem"));
				return;
			}
		}
		
		if (!StringUtils.isBlank(object.getAlertRole())) {
			Role role = Context.getUserService().getRole(object.getAlertRole());
			if (role == null) {
				errors.rejectValue("alertRole", String.format(
				    messageSourceService.getMessage("stockmanagement.stockrule.fieldvaluenotexist"), "alert role"));
				return;
			}
		}
		
		if (!StringUtils.isBlank(object.getMailRole())) {
			Role role = Context.getUserService().getRole(object.getMailRole());
			if (role == null) {
				errors.rejectValue("mailRole", String.format(
				    messageSourceService.getMessage("stockmanagement.stockrule.fieldvaluenotexist"), "mail role"));
				return;
			}
		}
	}
}
