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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;
import org.junit.jupiter.api.Test;

public class PropertiesUtilTest {
	
	@Test
	public void propertiesShouldRoundTripIncludingEscapesAndUnicode() throws Exception {
		Properties properties = new Properties();
		properties.setProperty("key = value", "line 1\nline 2\t\\\u03bb");
		properties.setProperty("empty", "");
		assertEquals(properties, PropertiesUtil.fromString(PropertiesUtil.toString(properties, "job state")));
	}
	
	@Test
	public void malformedEscapesShouldStillPropagate() {
		assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.fromString("key=" + "\\" + "uZZZZ"));
	}
}
