/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.module.stockmanagement.api.StockManagementException;

import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DateUtilTest {

	private TimeZone originalTimeZone;

	@BeforeEach
	public void setUp() {
		originalTimeZone = TimeZone.getDefault();
		// neither UTC (CI) nor a developer's likely zone, so a test passes only if the pin takes effect
		TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
	}

	@AfterEach
	public void tearDown() {
		TimeZone.setDefault(originalTimeZone);
	}

	@Test
	public void parseDate_shouldAcceptEveryOffsetForm() {
		for (String value : new String[] { "2026-11-18T07:00:00.000Z", "2026-11-18T10:00:00.000+0300",
		        "2026-11-18T10:00:00.000+03:00", "2026-11-18T10:00:00.000+03", "2026-11-18T02:00:00.000-0500",
		        "2026-11-18T07:00:00Z", "2026-11-18T10:00:00+03:00", "2026-11-18t07:00:00.000z" }) {
			assertEquals(Instant.parse("2026-11-18T07:00:00Z"), DateUtil.parseDate(value).toInstant(), value);
		}
	}

	@Test
	public void parseDate_shouldReadValuesWithoutAnOffsetInTheDefaultTimeZone() {
		for (String value : new String[] { "2026-11-18T10:00:00.000", "2026-11-18T10:00:00", "2026-11-18 10:00:00" }) {
			assertEquals(Instant.parse("2026-11-18T15:00:00Z"), DateUtil.parseDate(value).toInstant(), value);
		}
		assertEquals(Instant.parse("2026-11-18T05:00:00Z"), DateUtil.parseDate("2026-11-18").toInstant());
	}

	@Test
	public void parseDate_shouldAcceptSingleDigitFieldsAndShortFractions() {
		assertEquals(Instant.parse("2026-01-05T05:00:00Z"), DateUtil.parseDate("2026-1-5").toInstant());
		assertEquals(Instant.parse("2026-11-18T12:00:00Z"), DateUtil.parseDate("2026-11-18T7:00:00").toInstant());
		assertEquals(Instant.parse("2026-11-18T07:00:00.500Z"), DateUtil.parseDate("2026-11-18T07:00:00.5Z").toInstant());
	}

	@Test
	public void parseDate_shouldRejectInvalidValues() {
		for (String value : new String[] { "2026-02-30", "2026-02-29", "2026-13-01", "2026-11-18T24:00:00",
		        "2026-11-18T07:00:00.123456Z", "2026-11-18T07:00Z", "2026-11-18 07:00:00Z", "garbage", "" }) {
			assertThrows(StockManagementException.class, () -> DateUtil.parseDate(value), value);
		}
	}

	@Test
	public void parseDate_shouldRejectALocalTimeSkippedByDaylightSaving() {
		// clocks in New York went from 02:00 to 03:00 on 8 March 2026
		assertThrows(StockManagementException.class, () -> DateUtil.parseDate("2026-03-08T02:30:00"));
		// the same wall time with an explicit offset is a real instant
		assertEquals(Instant.parse("2026-03-08T02:30:00Z"), DateUtil.parseDate("2026-03-08T02:30:00Z").toInstant());
	}

	@Test
	public void parseDate_shouldReadADateAsTheFirstMomentOfADayWhoseMidnightIsSkipped() {
		// clocks in Cairo went from 00:00 to 01:00 (UTC+3) on 24 April 2026
		TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"));
		assertEquals(Instant.parse("2026-04-23T22:00:00Z"), DateUtil.parseDate("2026-04-24").toInstant());
		assertThrows(StockManagementException.class, () -> DateUtil.parseDate("2026-04-24 00:30:00"));
	}

	@Test
	public void formatDateForJson_shouldPrintTheDefaultTimeZoneOffset() {
		Date date = Date.from(Instant.parse("2026-11-18T15:00:00.123Z"));
		assertEquals("2026-11-18T10:00:00.123-0500", DateUtil.formatDateForJson(date));

		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		assertEquals("2026-11-18T15:00:00.123+0000", DateUtil.formatDateForJson(date));
	}

	@Test
	public void formatDateForJson_shouldProduceAValueThatParseDateReadsBack() {
		Date date = Date.from(Instant.parse("2026-11-18T15:00:00.123Z"));
		assertEquals(date, DateUtil.parseDate(DateUtil.formatDateForJson(date)));
	}
}
