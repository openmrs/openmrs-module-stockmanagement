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

public class ModuleConstants {
	
	/**
	 * Module ID
	 */
	public static final String MODULE_ID = "stockmanagement";
	
	public static final String SPA_INDEX_RESOURCE_PATH = "spa/index.html";
	
	public static final String DEV_ENVIRONMENT = "dev";
	
	public static final String DASHBOARD_EXTENSION_POINT_ID = MODULE_ID + ".dashboard";
	
	public static final String APP_DATA_WORKING_DIR = "stockmgmt";
	
	/**
	 * Display pattern for stock quantities in alerts and notifications, e.g. 1,234.57
	 */
	public static final String QUANTITY_FORMAT = "#,###,###,##0.##";
	
}
