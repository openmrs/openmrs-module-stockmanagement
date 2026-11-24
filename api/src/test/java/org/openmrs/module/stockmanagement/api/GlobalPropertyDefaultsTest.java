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
import org.openmrs.GlobalProperty;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.AdministrationServiceImpl;

/** Verifies compatibility against the actual core converter, including its error logging. */
public class GlobalPropertyDefaultsTest extends BaseModuleContextSensitiveTest {
	
	private AdministrationService service;
	
	private final List<LogEvent> events = new ArrayList<>();
	
	private Logger logger;
	
	private Level originalLevel;
	
	private AbstractAppender appender;
	
	@BeforeEach
	public void setUp() {
		service = Context.getAdministrationService();
		logger = (Logger) LogManager.getLogger(AdministrationServiceImpl.class);
		originalLevel = logger.getLevel();
		appender = new AbstractAppender("core-property-errors", null, PatternLayout.createDefaultLayout(), false) {
			
			@Override
			public void append(LogEvent event) {
				events.add(event.toImmutable());
			}
		};
		appender.start();
		logger.addAppender(appender);
		logger.setLevel(Level.ERROR);
	}
	
	@AfterEach
	public void tearDown() {
		logger.removeAppender(appender);
		logger.setLevel(originalLevel);
		appender.stop();
	}
	
	private void setProperty(String key, String value) {
		GlobalProperty property = service.getGlobalPropertyObject(key);
		if (property == null) {
			property = service.saveGlobalProperty(new GlobalProperty(key, "initial"));
		}
		// Mutate the managed property directly: saveGlobalProperty normalizes whitespace.
		// These tests exercise reading legacy values, including untrimmed and empty strings.
		property.setPropertyValue(value);
		assertEquals(value, service.getGlobalProperty(key));
	}
	
	static Stream<Arguments> numericDefaults() {
		return Stream.of(Arguments.of(EXCESS_RECIEVED_ITEM_THRESHOLD, BigDecimal.valueOf(10)),
		    Arguments.of(STOCK_ITEMS_MAX_UPLOAD_FILE_SIZE, 2L), Arguments.of(STOCK_RULE_JOB_BATCH_SIZE, 100),
		    Arguments.of(STOCK_BATCH_DEFAULT_EXPIRY_NOTIFICATION_NOTICE_PERIOD, 120),
		    Arguments.of(BATCH_JOB_EXPIRY_IN_MINUTES, 1440), Arguments.of(REPORTING_RECORDS_PAGE_SIZE, 1000),
		    Arguments.of(REPORTING_CALCULATIONS_NO_DAYS_IN_MONTH, BigDecimal.valueOf(30.5)));
	}
	
	@ParameterizedTest
	@MethodSource("numericDefaults")
	public void malformedNumbersShouldRetainDefaultsAndLog(String key, Object defaultValue) {
		for (String value : new String[] { "invalid", " 42 ", "1,000" }) {
			setProperty(key, value);
			Object actual = service.getGlobalPropertyValue(key, defaultValue);
			assertEquals(defaultValue, actual);
			assertEquals(defaultValue.getClass(), actual.getClass());
			assertTrue(events.stream().anyMatch(
			    event -> event.getLevel() == Level.ERROR && event.getMessage().getFormattedMessage().contains(value)));
		}
	}
	
	@ParameterizedTest
	@MethodSource("numericDefaults")
	public void missingAndEmptyNumbersShouldRetainDefaultsAndTypes(String key, Object defaultValue) {
		for (String value : new String[] { null, "" }) {
			setProperty(key, value);
			Object actual = service.getGlobalPropertyValue(key, defaultValue);
			assertEquals(defaultValue, actual);
			assertEquals(defaultValue.getClass(), actual.getClass());
		}
	}
	
	@Test
	public void uploadLimitShouldRemainALong() {
		setProperty(STOCK_ITEMS_MAX_UPLOAD_FILE_SIZE, "2147483648");
		assertEquals(2147483648L, service.getGlobalPropertyValue(STOCK_ITEMS_MAX_UPLOAD_FILE_SIZE, 2L));
	}
	
	@Test
	public void ordinaryBooleansShouldKeepBooleanParseBooleanSemantics() {
		for (String key : new String[] { NEGATIVE_STOCK_BALANCE_ALLOWED, ALLOW_STOCK_ISSUE_WITHOUT_REQUISITION }) {
			for (String value : new String[] { null, "", "true", "TrUe", "false", "yes", "1", " true ", "invalid" }) {
				setProperty(key, value);
				assertEquals(Boolean.parseBoolean(value), service.getGlobalPropertyValue(key, false));
			}
		}
	}
	
	@Test
	public void numericBoundsShouldRetainClampingRatherThanSubstituteDefaults() {
		setProperty(STOCK_RULE_JOB_BATCH_SIZE, "-1");
		setProperty(STOCK_BATCH_DEFAULT_EXPIRY_NOTIFICATION_NOTICE_PERIOD, "-1");
		setProperty(BATCH_JOB_EXPIRY_IN_MINUTES, "-1");
		assertEquals(1, Math.max(1, service.getGlobalPropertyValue(STOCK_RULE_JOB_BATCH_SIZE, 100)));
		assertEquals(0,
		    Math.max(0, service.getGlobalPropertyValue(STOCK_BATCH_DEFAULT_EXPIRY_NOTIFICATION_NOTICE_PERIOD, 120)));
		assertEquals(1440, Math.max(1440, service.getGlobalPropertyValue(BATCH_JOB_EXPIRY_IN_MINUTES, 1440)));
	}
	
	@Test
	public void coreTypedApiCannotRepresentNullableDefaults() {
		assertThrows(IllegalArgumentException.class, () -> service.getGlobalPropertyValue(UNKNOWN_CONCEPT_ID, null));
	}
	
	static Stream<String> nullableTextKeys() {
		return Stream.of(UNKNOWN_CONCEPT_ID, HEALTH_CENTER_NAME, STOCK_OPERATION_NOTIFICATION_EMAIL,
		    STOCK_OPERATION_NOTIFICATION_ROLE, OBSERVATION_DISPENSING_LOCATION_CONCEPT, OBSERVATION_DRUG_CONCEPT);
	}
	
	@ParameterizedTest
	@MethodSource("nullableTextKeys")
	public void nullableTextShouldPreserveMissingEmptyWhitespaceAndCommas(String key) {
		for (String value : new String[] { null, "", " ", "  first,second  " }) {
			setProperty(key, value);
			assertEquals(value, service.getGlobalProperty(key, null));
		}
	}
	
}
