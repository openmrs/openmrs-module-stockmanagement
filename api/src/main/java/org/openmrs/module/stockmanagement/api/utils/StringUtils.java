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

import java.util.regex.Pattern;

public class StringUtils {
	
	static Pattern emailPattern = Pattern
	        .compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*+@(?:[a-zA-Z0-9-]+\\.)++[a-zA-Z]{2,7}$");
	
	public static boolean isValidEmail(String email) {
		if (email == null)
			return false;
		if (email.endsWith("."))
			return false;
		return emailPattern.matcher(email).matches();
	}
}
