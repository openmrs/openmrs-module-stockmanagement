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

import org.openmrs.module.stockmanagement.api.ModuleConstants;
import org.openmrs.util.OpenmrsUtil;

import java.io.File;

public class FileUtil {
	
	public static File getWorkingDirectory() {
		File workingDirectory = OpenmrsUtil.getDirectoryInApplicationDataDirectory(ModuleConstants.APP_DATA_WORKING_DIR);
		return workingDirectory;
	}
	
	public static File getBatchJobFolder() {
		File folder = new File(getWorkingDirectory(), "batchjob");
		if (!folder.exists()) {
			folder.mkdirs();
		}
		return folder;
	}
}
