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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

public final class PropertiesUtil {
	
	private PropertiesUtil() {
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
	
}
