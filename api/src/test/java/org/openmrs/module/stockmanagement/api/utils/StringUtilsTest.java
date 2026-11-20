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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class StringUtilsTest {
	
	@Test
	public void isValidEmail_shouldAcceptWellFormedAddresses() {
		assertTrue(StringUtils.isValidEmail("first.last@example.com"));
		assertTrue(StringUtils.isValidEmail("user+tag@sub.domain.org"));
	}
	
	@Test
	public void isValidEmail_shouldRejectMalformedAddresses() {
		assertFalse(StringUtils.isValidEmail(null));
		assertFalse(StringUtils.isValidEmail("a..b@example.com"));
		assertFalse(StringUtils.isValidEmail("a@example..com"));
		assertFalse(StringUtils.isValidEmail("a@example.com."));
		assertFalse(StringUtils.isValidEmail("@example.com"));
	}
	
	@Test
	public void isValidEmail_shouldNotOverflowTheStackOnAVeryLongAddress() {
		StringBuilder localPart = new StringBuilder("a");
		for (int i = 0; i < 100000; i++) {
			localPart.append(".a");
		}
		assertTrue(StringUtils.isValidEmail(localPart + "@example.com"));
	}
	
	@Test
	public void isValidEmail_shouldNotOverflowTheStackOnAVeryLongDomain() {
		StringBuilder domain = new StringBuilder();
		for (int i = 0; i < 100000; i++) {
			domain.append("a.");
		}
		assertTrue(StringUtils.isValidEmail("a@" + domain + "com"));
	}
}
