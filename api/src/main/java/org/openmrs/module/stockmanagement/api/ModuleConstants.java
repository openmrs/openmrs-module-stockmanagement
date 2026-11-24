/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class ModuleConstants {
	
	/**
	 * Module ID
	 */
	public static final String MODULE_ID = "stockmanagement";
	
	public static final String APP_DATA_WORKING_DIR = "stockmgmt";
	
	public static final String STOCK_ITEM_SEARCH_MAX_INTERMEDIATE_RESULT = MODULE_ID
	        + ".stockItemSearchMaxIntermediateResult";
	
	public static final String NEGATIVE_STOCK_BALANCE_ALLOWED = MODULE_ID + ".negativeStockBalanceAllowed";
	
	public static final String EXCESS_RECIEVED_ITEM_THRESHOLD = MODULE_ID + ".excessReceivedItemThreshold";
	
	public static final String UNKNOWN_CONCEPT_ID = MODULE_ID + ".unknownConceptId";
	
	public static final String STOCK_ITEMS_MAX_UPLOAD_FILE_SIZE = MODULE_ID + ".stockItemsMaxUploadFileSize";
	
	public static final String HEALTH_CENTER_NAME = "ugandaemr.healthCenterName";
	
	public static final String ENABLE_STOCK_RULE_JOB = MODULE_ID + ".enableStockRuleJob";
	
	public static final String STOCK_RULE_JOB_BATCH_SIZE = MODULE_ID + ".stockRuleJobBatchSize";
	
	public static final String ENABLE_STOCK_BATCH_JOB = MODULE_ID + ".enableStockBatchJob";
	
	public static final String STOCK_BATCH_DEFAULT_EXPIRY_NOTIFICATION_NOTICE_PERIOD = MODULE_ID
	        + ".stockBatchDefaultExpiryNotificationNoticePeriod";
	
	public static final String STOCK_OPERATION_NOTIFICATION_EMAIL = MODULE_ID + ".stockOperationNotificationEmail";
	
	public static final String STOCK_OPERATION_NOTIFICATION_ROLE = MODULE_ID + ".stockOperationNotificationRole";
	
	public static final String BATCH_JOB_EXPIRY_IN_MINUTES = MODULE_ID + ".batchJobExpiryInMinutes";
	
	public static final String ALLOW_STOCK_ISSUE_WITHOUT_REQUISITION = MODULE_ID + ".allowStockIssueWithoutRequisition";
	
	public static final String REPORTING_RECORDS_PAGE_SIZE = MODULE_ID + ".reportingRecordsPageSize";
	
	public static final String REPORTING_CALCULATIONS_NO_DAYS_IN_MONTH = MODULE_ID + ".reportingCalculationsNoDaysInMonth";
	
	public static final String OBSERVATION_DISPENSING_LOCATION_CONCEPT = MODULE_ID + ".observationDispensingLocationConcept";
	
	public static final String OBSERVATION_DRUG_CONCEPT = MODULE_ID + ".observationDrugConcept";
	
	public static final String PACKAGING_UNIT_PACK_SIZE_PRIORITY_IS_BIG_TO_SMALL = MODULE_ID
	        + ".packagingUnitPackSizePriorityIsBigToSmall";
	
	private static final Logger log = LoggerFactory.getLogger(ModuleConstants.class);
	
	/** Core's typed property API requires a non-null default, so nullable integers need parsing. */
	public static Integer parseOptionalInteger(String key, String value) {
		if (StringUtils.isEmpty(value)) {
			return null;
		}
		try {
			return Integer.valueOf(value);
		}
		catch (NumberFormatException exception) {
			log.warn("Invalid value '{}' for global property '{}'; using null", value, key);
			return null;
		}
	}
	
	/** Job switches historically also accept yes and 1, without trimming. */
	public static boolean parseJobEnabled(String key, String value) {
		if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value)) {
			return true;
		}
		if (!StringUtils.isEmpty(value) && !"false".equalsIgnoreCase(value) && !"no".equalsIgnoreCase(value)
		        && !"0".equals(value)) {
			log.warn("Invalid value '{}' for global property '{}'; using false", value, key);
		}
		return false;
	}
	
	/** Blank values mean true, but all other values retain Boolean.parseBoolean semantics. */
	public static boolean parseUomPriority(String value) {
		if (StringUtils.isBlank(value)) {
			return true;
		}
		if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
			log.warn("Invalid value '{}' for global property '{}'; using false", value,
			    PACKAGING_UNIT_PACK_SIZE_PRIORITY_IS_BIG_TO_SMALL);
		}
		return Boolean.parseBoolean(value);
	}
	
	/** Keeps the setting's lower bound, and logs when a configured value is raised to it. */
	public static int atLeast(String key, int value, int minimum) {
		if (value < minimum) {
			log.warn("Invalid value '{}' for global property '{}'; using {}", value, key, minimum);
			return minimum;
		}
		return value;
	}
	
	public static Integer positiveValue(String key, Integer value, Integer defaultValue) {
		if (value <= 0) {
			log.warn("Invalid value '{}' for global property '{}'; using {}", value, key, defaultValue);
			return defaultValue;
		}
		return value;
	}
	
	public static BigDecimal positiveValue(String key, BigDecimal value, BigDecimal defaultValue) {
		if (value.compareTo(BigDecimal.ZERO) <= 0) {
			log.warn("Invalid value '{}' for global property '{}'; using {}", value, key, defaultValue);
			return defaultValue;
		}
		return value;
	}
}
