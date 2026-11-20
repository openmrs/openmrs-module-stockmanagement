/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.dto.reporting;

public enum Fullfillment {
	
	All(),
	Full(),
	Partial(),
	None;
	
	public static Fullfillment findByName(String name) {
		return findInList(name, values());
	}
	
	public static Fullfillment findInList(String name, Fullfillment[] parameterList) {
		Fullfillment result = null;
		for (Fullfillment enumValue : parameterList) {
			if (enumValue.name().equalsIgnoreCase(name)) {
				result = enumValue;
				break;
			}
		}
		return result;
	}
}
