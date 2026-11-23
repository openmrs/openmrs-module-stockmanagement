/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement;

import org.junit.jupiter.api.Test;
import org.openmrs.module.stockmanagement.api.ModuleConstants;
import org.openmrs.module.stockmanagement.api.reporting.ReportGenerator;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HelperFormattingTest {
	
	@Test
	public void shouldPreserveQuantityDisplayAndHalfEvenRounding() {
		Locale previous = Locale.getDefault(Locale.Category.FORMAT);
		try {
			Locale.setDefault(Locale.Category.FORMAT, Locale.US);
			String[][] examples = { { "0", "0" }, { "-1234.567", "-1,234.57" },
			        { "12345678901234567890.125", "12,345,678,901,234,567,890.12" }, { "1.235", "1.24" },
			        { "1.225", "1.22" }, { "12.5000", "12.5" }, { "0.001", "0" }, { "-0.001", "-0" } };
			for (String[] example : examples) {
				assertEquals(example[1],
				    new DecimalFormat(ModuleConstants.QUANTITY_FORMAT).format(new BigDecimal(example[0])), example[0]);
			}
			assertThrows(IllegalArgumentException.class,
			    () -> new DecimalFormat(ModuleConstants.QUANTITY_FORMAT).format((Object) null));
		}
		finally {
			Locale.setDefault(Locale.Category.FORMAT, previous);
		}
	}
	
	@Test
	public void shouldPreserveIntegerDisplay() {
		Locale previous = Locale.getDefault(Locale.Category.FORMAT);
		try {
			Locale.setDefault(Locale.Category.FORMAT, Locale.US);
			ReportGenerator report = new ReportGenerator() {};
			Properties properties = new Properties();
			int[][] examples = { { 0 }, { -1234 }, { Integer.MAX_VALUE } };
			String[] expected = { "0", "-1,234", "2,147,483,647" };
			for (int i = 0; i < examples.length; i++) {
				report.setRecordsProcessed(properties, examples[i][0]);
				assertEquals(expected[i], properties.getProperty("param.RecordsProcessed.value.desc"));
			}
		}
		finally {
			Locale.setDefault(Locale.Category.FORMAT, previous);
		}
	}
	
	@Test
	public void shouldPreserveReportParameterDisplay() {
		Locale previous = Locale.getDefault(Locale.Category.FORMAT);
		try {
			Locale.setDefault(Locale.Category.FORMAT, Locale.US);
			ReportGenerator report = new ReportGenerator() {};
			Properties properties = new Properties();
			report.setRecordsProcessed(properties, 1234567);
			assertEquals("1234567", properties.getProperty("param.RecordsProcessed.value"));
			assertEquals("1,234,567", properties.getProperty("param.RecordsProcessed.value.desc"));
			report.setCurrentPageIndex(properties, 999);
			assertEquals("1,000", properties.getProperty("param.CurrentPageIndex.value.desc"));
		}
		finally {
			Locale.setDefault(Locale.Category.FORMAT, previous);
		}
	}
}
