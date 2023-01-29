/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.page.controller;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.api.ModuleConstants;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.utils.Pair;
import org.openmrs.module.stockmanagement.api.utils.GlobalProperties;

import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageModel;
import org.openmrs.ui.framework.page.PageRequest;
import org.openmrs.ui.framework.resource.ResourceFactory;
import org.openmrs.ui.framework.resource.ResourceProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpaPageController {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	private static final Pattern INDEX_FILE_PARSER_REG = Pattern.compile("<head>(.*)</head>\\s*<body>(.*)</body>",
	    Pattern.CASE_INSENSITIVE);
	
	private static String HeadContent = null;
	
	private static String BodyContent = null;
	
	/**
	 * Initially called after the getUsers method to get the landing form name
	 * 
	 * @return String form view name
	 */
	public void controller(UiUtils ui, PageRequest pageRequest, PageModel model) {
		try {
			pageRequest.getResponse().addHeader("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
		}
		catch (Exception exception) {}
		String contextPath = ui.contextPath();
		readIndexHtml(model, contextPath);
	}
	
	private void readIndexHtml(PageModel model, String contextPath) {
		
		model.addAttribute("stockmgmtBaseUrl", "/" + contextPath);
		String resourcesBaseUrl = "/" + contextPath + "/moduleResources/" + ModuleConstants.MODULE_ID + "/spa";
		model.addAttribute("stockmgmtResourcesUrl", resourcesBaseUrl);
		model.addAttribute("spaHead", "");
		model.addAttribute("spaBody", "");
		model.addAttribute("stockSourceTypeConceptId", GlobalProperties.getStockSourceCodeConceptId());
		model.addAttribute("stockAdjustmentReasonConceptId", GlobalProperties.getStockAdjustmentReasonCodeConceptId());
		model.addAttribute("dispensingUnitsConceptId", GlobalProperties.getDispensingUnitsConceptId());
		model.addAttribute("packagingUnitsConceptId", GlobalProperties.getPackagingUnitsConceptId());
		
		model.addAttribute("stockOperationPrintDisableBalanceOnHand",
		    GlobalProperties.disableBalanceOnHandOnStockOperationPrint());
		model.addAttribute("stockOperationPrintDisableCosts", GlobalProperties.disableCostsOnStockOperationPrint());
		model.addAttribute("stockItemCategoryConceptId", GlobalProperties.getStockItemCategoryConceptId());
		model.addAttribute("allowStockIssueWithoutRequisition", GlobalProperties.allowStockIssueWithoutRequisition());
		
		String healthCenterName = Context.getService(StockManagementService.class).getHealthCenterName();
		model.addAttribute("healthCenterName", healthCenterName);
		
		String setting = GlobalProperties.getPrintLogo();
		model.addAttribute("printLogo", setting == null ? "" : setting);
		setting = GlobalProperties.getPrintLogoText();
		model.addAttribute("printLogoText", setting == null ? "" : setting);
		model.addAttribute("closePrintAfterPrint", GlobalProperties.closePrintAfterPrint());
		
		boolean isDevEnv = GlobalProperties.isDevelopment();
		model.addAttribute("isDevEnv", isDevEnv);
		boolean useCache = !isDevEnv;
		if (useCache && BodyContent != null) {
			model.replace("spaHead", HeadContent);
			model.replace("spaBody", BodyContent);
			return;
		}
		
		ResourceFactory resourceFactory = ResourceFactory.getInstance();
		ResourceProvider resourceProvider = resourceFactory.getResourceProviders().get(ModuleConstants.MODULE_ID);
		final File indexFile = resourceProvider.getResource(ModuleConstants.SPA_INDEX_RESOURCE_PATH);
		if (indexFile == null || !indexFile.isFile()) {
			log.error("Spa index file " + ModuleConstants.MODULE_ID + ": " + ModuleConstants.SPA_INDEX_RESOURCE_PATH
			        + " not found or is not a file.");
			return;
		}
		
		String indexFileContents = null;
		try {
			indexFileContents = new String(Files.readAllBytes(indexFile.toPath()));
		}
		catch (IOException e) {
			log.error("Error reading spa index file " + ModuleConstants.MODULE_ID + ": "
			        + ModuleConstants.SPA_INDEX_RESOURCE_PATH, e);
			return;
		}
		
		indexFileContents = indexFileContents.replace("/%STOCKMGMT_BASE_URL%", resourcesBaseUrl);
		Matcher matcher = INDEX_FILE_PARSER_REG.matcher(indexFileContents);
		if (!matcher.find()) {
			log.error("Spa index file " + ModuleConstants.MODULE_ID + ": " + ModuleConstants.SPA_INDEX_RESOURCE_PATH
			        + "failed to match pattern");
			return;
		}
		
		String head = matcher.group(1);
		String body = matcher.group(2);
		if (useCache) {
			HeadContent = head;
			BodyContent = body;
		}
		model.replace("spaHead", head);
		model.replace("spaBody", body);
	}
}
