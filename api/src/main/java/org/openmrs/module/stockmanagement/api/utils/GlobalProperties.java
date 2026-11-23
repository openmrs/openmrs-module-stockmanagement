/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.ModuleConstants;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Properties;

public class GlobalProperties {
	
	/**
	 * Logger for this class and subclasses
	 */
	protected final Log log = LogFactory.getLog(getClass());
	
	public static final String STOCK_ITEM_SEARCH_MAX_INTERMEDIATE_RESULT = ModuleConstants.MODULE_ID
	        + ".stockItemSearchMaxIntermediateResult";
	
	public static final String NEGATIVE_STOCK_BALANCE_ALLOWED = ModuleConstants.MODULE_ID + ".negativeStockBalanceAllowed";
	
	public static final String EXCESS_RECIEVED_ITEM_THRESHOLD = ModuleConstants.MODULE_ID + ".excessReceivedItemThreshold";
	
	public static final String UNKNOWN_CONCEPT_ID = ModuleConstants.MODULE_ID + ".unknownConceptId";
	
	public static final String STOCK_ITEMS_MAX_UPLOAD_FILE_SIZE = ModuleConstants.MODULE_ID + ".stockItemsMaxUploadFileSize";
	
	public static final String HEALTH_CENTER_NAME = "ugandaemr.healthCenterName";
	
	public static final String ENABLE_STOCK_RULE_JOB = ModuleConstants.MODULE_ID + ".enableStockRuleJob";
	
	public static final String STOCK_RULE_JOB_BATCH_SIZE = ModuleConstants.MODULE_ID + ".stockRuleJobBatchSize";
	
	public static final String ENABLE_STOCK_BATCH_JOB = ModuleConstants.MODULE_ID + ".enableStockBatchJob";
	
	public static final String STOCK_BATCH_DEFAULT_EXPIRY_NOTIFICATION_NOTICE_PERIOD = ModuleConstants.MODULE_ID
	        + ".stockBatchDefaultExpiryNotificationNoticePeriod";
	
	public static final String STOCK_OPERATION_NOTIFICATION_EMAIL = ModuleConstants.MODULE_ID
	        + ".stockOperationNotificationEmail";
	
	public static final String STOCK_OPERATION_NOTIFICATION_ROLE = ModuleConstants.MODULE_ID
	        + ".stockOperationNotificationRole";
	
	public static final String BATCH_JOB_EXPIRY_IN_MINUTES = ModuleConstants.MODULE_ID + ".batchJobExpiryInMinutes";
	
	public static String getGlobalProperty(String property) {
		return Context.getAdministrationService().getGlobalProperty(property);
	}
	
	public static Integer getStockItemSearchMaxDrugConceptIntermediateResult() {
		try {
			return Integer.parseInt(getGlobalProperty(STOCK_ITEM_SEARCH_MAX_INTERMEDIATE_RESULT));
		}
		catch (Exception exception) {}
		return null;
	}
	
	public static boolean getNegativeStockBalanceAllowed() {
		try {
			return Boolean.parseBoolean(getGlobalProperty(NEGATIVE_STOCK_BALANCE_ALLOWED));
		}
		catch (Exception exception) {}
		return false;
	}
	
	public static BigDecimal getExcessReceivedItemThreshold() {
		try {
			return new BigDecimal(getGlobalProperty(EXCESS_RECIEVED_ITEM_THRESHOLD));
		}
		catch (Exception exception) {}
		return BigDecimal.valueOf(10);
	}
	
	public static String getUnknownConceptId() {
		try {
			return getGlobalProperty(UNKNOWN_CONCEPT_ID);
		}
		catch (Exception exception) {}
		return null;
	}
	
	public static long getStockItemsMaxUploadSize() {
		
		try {
			return Long.parseLong(getGlobalProperty(STOCK_ITEMS_MAX_UPLOAD_FILE_SIZE));
		}
		catch (Exception exception) {}
		return 2;
		
	}
	
	public static String getHealthCenterName() {
		try {
			return getGlobalProperty(HEALTH_CENTER_NAME);
		}
		catch (Exception exception) {}
		return null;
	}
	
	public static boolean isStockRuleJobEnabled() {
		try {
			String result = getGlobalProperty(ENABLE_STOCK_RULE_JOB);
			if (result == null)
				return false;
			return result.equalsIgnoreCase("true") || result.equalsIgnoreCase("yes") || result.equals("1");
		}
		catch (Exception exception) {}
		return false;
	}
	
	public static boolean isStockBatchJobEnabled() {
		try {
			String result = getGlobalProperty(ENABLE_STOCK_BATCH_JOB);
			if (result == null)
				return false;
			return result.equalsIgnoreCase("true") || result.equalsIgnoreCase("yes") || result.equals("1");
		}
		catch (Exception exception) {}
		return false;
	}
	
	public static Integer getStockRuleJobBatchSize() {
		try {
			return Math.max(1, Integer.parseInt(getGlobalProperty(STOCK_RULE_JOB_BATCH_SIZE)));
		}
		catch (Exception exception) {}
		return 100;
	}
	
	public static Integer getStockBatchDefaultExpiryNotificationNoticePeriod() {
		try {
			return Math.max(0, Integer.parseInt(getGlobalProperty(STOCK_BATCH_DEFAULT_EXPIRY_NOTIFICATION_NOTICE_PERIOD)));
		}
		catch (Exception exception) {}
		return 120;
	}
	
	public static boolean allowStockIssueWithoutRequisition() {
		try {
			return Boolean.parseBoolean(getGlobalProperty("stockmanagement.allowStockIssueWithoutRequisition"));
		}
		catch (Exception exception) {}
		return false;
	}
	
	public static String getStockOperationNotificationEmail() {
		try {
			return getGlobalProperty(STOCK_OPERATION_NOTIFICATION_EMAIL);
		}
		catch (Exception exception) {}
		return null;
	}
	
	public static String getStockOperationNotificationRole() {
		try {
			return getGlobalProperty(STOCK_OPERATION_NOTIFICATION_ROLE);
		}
		catch (Exception exception) {}
		return null;
	}
	
	public static Integer getBatchJobExpiryInMinutes() {
		try {
			return Math.max(1440, Integer.parseInt(getGlobalProperty(BATCH_JOB_EXPIRY_IN_MINUTES)));
		}
		catch (Exception exception) {}
		return 1440;
	}
	
	public static String toString(Properties properties, String description) throws IOException {
		try (StringWriter stringWriter = new StringWriter()) {
			properties.store(stringWriter, description);
			return stringWriter.toString();
		}
	}
	
	public static Properties fromString(String propertiesString) throws IOException {
		final Properties properties = new Properties();
		try (StringReader stringReader = new StringReader(propertiesString)) {
			properties.load(stringReader);
			return properties;
		}
	}
	
	public static Integer GetReportingRecordsPageSize() {
		try {
			Integer result = Integer.parseInt(getGlobalProperty("stockmanagement.reportingRecordsPageSize"));
			return result <= 0 ? 1000 : result;
		}
		catch (Exception exception) {}
		return 1000;
	}
	
	public static BigDecimal GetReportingCalculationsNoDaysInMonth() {
		try {
			BigDecimal result = new BigDecimal(getGlobalProperty("stockmanagement.reportingCalculationsNoDaysInMonth"));
			return result.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.valueOf(30.5) : result;
		}
		catch (Exception exception) {}
		return BigDecimal.valueOf(30.5);
	}
	
	public static String getObservationDispensingLocationConcept() {
		try {
			return getGlobalProperty("stockmanagement.observationDispensingLocationConcept");
		}
		catch (Exception exception) {}
		return null;
	}
	
	public static String getObservationDrugConcept() {
		try {
			return getGlobalProperty("stockmanagement.observationDrugConcept");
		}
		catch (Exception exception) {}
		return null;
	}
	
	public static boolean uomPriorityIsBigToSmall() {
		try {
			String property = getGlobalProperty("stockmanagement.packagingUnitPackSizePriorityIsBigToSmall");
			if (StringUtils.isBlank(property))
				return true;
			return Boolean.parseBoolean(property);
		}
		catch (Exception exception) {}
		return true;
	}
}
