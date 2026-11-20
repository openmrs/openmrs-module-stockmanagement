/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.stockmanagement.api.reporting;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportGeneratorCsvTest {
	
	@TempDir
	Path directory;
	
	private final ReportGenerator report = new ReportGenerator() {};
	
	@Test
	void shouldPreserveReportCsvBytes() throws IOException {
		Path output = directory.resolve("report.csv");
		// Use the same UTF-8 file writer as the reports, including their append mode.
		try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
		    StandardOpenOption.APPEND)) {
			report.writeLineToCsv(report.createCsvWriter(writer), "Item", "Batch", "Remarks", "Quantity", "Unit");
			report.writeLineToCsv(report.createCsvWriter(writer), "Paracetamol, 500 mg", "B\"42", "First\nSecond", "12",
			    null);
			report.writeLineToCsv(report.createCsvWriter(writer), "Caf\u00e9 \u836f", null, "CR\rLF\r\nend", "",
			    " tablets ");
		}
		try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
			report.writeLineToCsv(report.createCsvWriter(writer), "", "", null, "0", "\tunit");
		}
		
		String expected = "Item,Batch,Remarks,Quantity,Unit\n"
		        + "\"Paracetamol, 500 mg\",\"B\"\"42\",\"First\nSecond\",12,\n"
		        + "Caf\u00e9 \u836f,,\"CR\rLF\r\nend\",, tablets \n" + ",,,0,\tunit\n";
		// The complete byte assertion also excludes a BOM and platform-specific CRLF separators.
		assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(output));
	}
	
	@Test
	void shouldPreserveEmptyAndNullRecordBytes() throws IOException {
		Path output = directory.resolve("empty.csv");
		try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
			report.writeLineToCsv(report.createCsvWriter(writer), (String[]) null);
			report.writeLineToCsv(report.createCsvWriter(writer));
			report.writeLineToCsv(report.createCsvWriter(writer), "");
			report.writeLineToCsv(report.createCsvWriter(writer), (String) null);
		}
		assertArrayEquals("\n\n\n".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(output));
	}
}
