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

import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.util.OpenmrsUtil;

import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
	
	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	
	/**
	 * The formats parseDate accepts, tried in order: yyyy-MM-dd'T'HH:mm:ss with optional milliseconds
	 * and an optional offset (Z, +03:00, +0300 or +03), yyyy-MM-dd HH:mm:ss, and yyyy-MM-dd. Month, day
	 * and time fields may have one or two digits. A value without an offset is read in the default time
	 * zone, and a date on its own is the start of that day, which is the first moment after the change
	 * on a day when daylight saving starts at midnight. Invalid dates such as 2026-02-30, and local
	 * times that a daylight saving change skips, are rejected rather than adjusted.
	 */
	private static final DateTimeFormatter[] PARSE_FORMATS = {
	        parseFormat(time(date().appendLiteral('T')).optionalStart().appendLiteral('.')
	                .appendFraction(ChronoField.NANO_OF_SECOND, 1, 3, false).optionalEnd().appendPattern("[XXX][X]")),
	        parseFormat(time(date().appendLiteral(' '))), parseFormat(date()) };
	
	private static final DateTimeFormatter JSON_FORMAT = DateTimeFormatter.ofPattern(DATE_FORMAT);
	
	private static DateTimeFormatterBuilder date() {
		return new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("uuuu").appendLiteral('-')
		        .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral('-')
		        .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE);
	}
	
	private static DateTimeFormatterBuilder time(DateTimeFormatterBuilder builder) {
		return builder.appendValue(ChronoField.HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(':')
		        .appendValue(ChronoField.MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(':')
		        .appendValue(ChronoField.SECOND_OF_MINUTE, 1, 2, SignStyle.NOT_NEGATIVE);
	}
	
	private static DateTimeFormatter parseFormat(DateTimeFormatterBuilder builder) {
		return builder.toFormatter().withResolverStyle(ResolverStyle.STRICT);
	}
	
	private static Date toDate(LocalDateTime localDateTime) {
		ZonedDateTime zoned = localDateTime.atZone(ZoneId.systemDefault());
		if (!zoned.toLocalDateTime().equals(localDateTime)) {
			throw new DateTimeException(localDateTime + " does not exist in time zone " + zoned.getZone());
		}
		return Date.from(zoned.toInstant());
	}
	
	public static Date today() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}
	
	public static String formatDDMMMyyyy(Date date) {
		if (date == null) {
			return null;
		}
		return new SimpleDateFormat("dd-MMM-yyyy").format(date);
	}
	
	public static Date parseDate(String dateValue) {
		DateTimeException pex = null;
		for (DateTimeFormatter format : PARSE_FORMATS) {
			try {
				TemporalAccessor parsed = format.parseBest(dateValue, OffsetDateTime::from, LocalDateTime::from,
				    LocalDate::from);
				if (parsed instanceof OffsetDateTime) {
					return Date.from(((OffsetDateTime) parsed).toInstant());
				}
				if (parsed instanceof LocalDateTime) {
					return toDate((LocalDateTime) parsed);
				}
				return Date.from(((LocalDate) parsed).atStartOfDay(ZoneId.systemDefault()).toInstant());
			}
			catch (DateTimeException ex) {
				pex = ex;
			}
		}
		throw new StockManagementException(
		        "Error converting date - correct format (ISO8601 Long): yyyy-MM-dd'T'HH:mm:ss.SSSZ", pex);
	}
	
	public static String formatDateForJson(Date dateValue) {
		return JSON_FORMAT.format(dateValue.toInstant().atZone(ZoneId.systemDefault()));
	}
	
	public static Date endOfDay(Date date) {
		if (date == null)
			return date;
		return OpenmrsUtil.getLastMomentOfDay(date);
	}
	
	public static String formatForFile(Date date) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		return formatter.format(date);
	}
}
