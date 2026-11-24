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

import static org.junit.jupiter.api.Assertions.*;
import static org.openmrs.module.stockmanagement.api.ModuleConstants.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class ModuleConstantsTest {
	
	private final List<LogEvent> events = new ArrayList<>();
	
	private Logger logger;
	
	private Level originalLevel;
	
	private AbstractAppender appender;
	
	@BeforeEach
	public void captureWarnings() {
		logger = (Logger) LogManager.getLogger(ModuleConstants.class);
		originalLevel = logger.getLevel();
		appender = new AbstractAppender("setting-warnings", null, PatternLayout.createDefaultLayout(), false) {
			
			@Override
			public void append(LogEvent event) {
				events.add(event.toImmutable());
			}
		};
		appender.start();
		logger.addAppender(appender);
		logger.setLevel(Level.WARN);
	}
	
	@AfterEach
	public void stopCapturingWarnings() {
		logger.removeAppender(appender);
		logger.setLevel(originalLevel);
		appender.stop();
	}
	
	private void assertWarning(String key, String value) {
		assertTrue(events.stream()
		        .anyMatch(event -> event.getLevel() == Level.WARN && event.getMessage().getFormattedMessage().contains(key)
		                && event.getMessage().getFormattedMessage().contains("'" + value + "'")));
	}
	
	@ParameterizedTest
	@ValueSource(strings = { "invalid", " 10", "10 ", "2147483648", "1.5" })
	public void optionalIntegerShouldWarnAndReturnNullForMalformedValues(String value) {
		assertNull(parseOptionalInteger(STOCK_ITEM_SEARCH_MAX_INTERMEDIATE_RESULT, value));
		assertWarning(STOCK_ITEM_SEARCH_MAX_INTERMEDIATE_RESULT, value);
	}
	
	@ParameterizedTest
	@NullAndEmptySource
	public void optionalIntegerShouldRetainNullDefault(String value) {
		assertNull(parseOptionalInteger(STOCK_ITEM_SEARCH_MAX_INTERMEDIATE_RESULT, value));
	}
	
	@ParameterizedTest
	@ValueSource(strings = { "-1", "0", "1", "2147483647", "-2147483648" })
	public void optionalIntegerShouldNotImposeANewLowerBound(String value) {
		assertEquals(Integer.valueOf(value), parseOptionalInteger(STOCK_ITEM_SEARCH_MAX_INTERMEDIATE_RESULT, value));
		assertTrue(events.isEmpty());
	}
	
	static Stream<Arguments> jobValues() {
		return Stream.of(ENABLE_STOCK_RULE_JOB, ENABLE_STOCK_BATCH_JOB)
		        .flatMap(key -> Stream.of(Arguments.of(key, "true", true), Arguments.of(key, "TrUe", true),
		            Arguments.of(key, "YES", true), Arguments.of(key, "yes", true), Arguments.of(key, "1", true),
		            Arguments.of(key, "false", false), Arguments.of(key, "NO", false), Arguments.of(key, "0", false),
		            Arguments.of(key, "", false), Arguments.of(key, null, false)));
	}
	
	@ParameterizedTest
	@MethodSource("jobValues")
	public void jobFlagsShouldRetainAliasesAndDefaults(String key, String value, boolean expected) {
		assertEquals(expected, parseJobEnabled(key, value));
		assertTrue(events.isEmpty());
	}
	
	static Stream<Arguments> malformedJobValues() {
		return Stream.of(ENABLE_STOCK_RULE_JOB, ENABLE_STOCK_BATCH_JOB).flatMap(
		    key -> Stream.of(Arguments.of(key, "invalid"), Arguments.of(key, " true "), Arguments.of(key, " yes ")));
	}
	
	@ParameterizedTest
	@MethodSource("malformedJobValues")
	public void jobFlagsShouldWarnAndReturnFalseForMalformedValues(String key, String value) {
		assertFalse(parseJobEnabled(key, value));
		assertWarning(key, value);
	}
	
	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t\n", "true", "TrUe" })
	public void uomPriorityShouldRetainTrueForBlankOrTrueValues(String value) {
		assertTrue(parseUomPriority(value));
		assertTrue(events.isEmpty());
	}
	
	@ParameterizedTest
	@ValueSource(strings = { "invalid", " true ", "yes", "1" })
	public void uomPriorityShouldWarnButRetainFalseForMalformedNonblankValues(String value) {
		// Unlike missing/blank values, malformed nonblank values historically mean false.
		assertFalse(parseUomPriority(value));
		assertWarning(PACKAGING_UNIT_PACK_SIZE_PRIORITY_IS_BIG_TO_SMALL, value);
	}
	
	@Test
	public void uomPriorityShouldAcceptFalseCaseInsensitively() {
		assertFalse(parseUomPriority("FaLsE"));
		assertTrue(events.isEmpty());
	}
	
	@ParameterizedTest
	@ValueSource(ints = { -1, 0, Integer.MIN_VALUE })
	public void ruleBatchSizeBelowOneShouldWarnAndUseOne(int value) {
		assertEquals(1, atLeast(STOCK_RULE_JOB_BATCH_SIZE, value, 1));
		assertWarning(STOCK_RULE_JOB_BATCH_SIZE, String.valueOf(value));
	}
	
	@Test
	public void batchJobExpiryBelowADayShouldWarnAndUseADay() {
		assertEquals(1440, atLeast(BATCH_JOB_EXPIRY_IN_MINUTES, 60, 1440));
		assertWarning(BATCH_JOB_EXPIRY_IN_MINUTES, "60");
	}
	
	@Test
	public void negativeExpiryNoticePeriodShouldWarnAndUseZero() {
		assertEquals(0, atLeast(STOCK_BATCH_DEFAULT_EXPIRY_NOTIFICATION_NOTICE_PERIOD, -5, 0));
		assertWarning(STOCK_BATCH_DEFAULT_EXPIRY_NOTIFICATION_NOTICE_PERIOD, "-5");
	}
	
	@Test
	public void valuesWithinTheLowerBoundShouldBeKeptWithoutWarning() {
		assertEquals(1, atLeast(STOCK_RULE_JOB_BATCH_SIZE, 1, 1));
		assertEquals(0, atLeast(STOCK_BATCH_DEFAULT_EXPIRY_NOTIFICATION_NOTICE_PERIOD, 0, 0));
		assertEquals(2000, atLeast(BATCH_JOB_EXPIRY_IN_MINUTES, 2000, 1440));
		assertTrue(events.isEmpty());
	}
	
	@ParameterizedTest
	@ValueSource(ints = { -1, 0, Integer.MIN_VALUE })
	public void reportingPageSizeShouldWarnAndFallBackForNonpositiveValues(int value) {
		assertEquals(1000, positiveValue(REPORTING_RECORDS_PAGE_SIZE, value, 1000));
		assertWarning(REPORTING_RECORDS_PAGE_SIZE, String.valueOf(value));
	}
	
	@ParameterizedTest
	@ValueSource(strings = { "-1", "0", "0.000", "-0.5" })
	public void reportingDaysShouldWarnAndFallBackForNonpositiveValues(String value) {
		BigDecimal days = new BigDecimal(value);
		assertEquals(BigDecimal.valueOf(30.5),
		    positiveValue(REPORTING_CALCULATIONS_NO_DAYS_IN_MONTH, days, BigDecimal.valueOf(30.5)));
		assertWarning(REPORTING_CALCULATIONS_NO_DAYS_IN_MONTH, days.toString());
	}
	
	@Test
	public void positiveReportingValuesShouldRetainTheirTypesAndPrecision() {
		assertEquals(1, positiveValue(REPORTING_RECORDS_PAGE_SIZE, 1, 1000));
		BigDecimal days = new BigDecimal("0.1234567890123456789");
		assertSame(days, positiveValue(REPORTING_CALCULATIONS_NO_DAYS_IN_MONTH, days, BigDecimal.valueOf(30.5)));
		assertTrue(events.isEmpty());
	}
}
