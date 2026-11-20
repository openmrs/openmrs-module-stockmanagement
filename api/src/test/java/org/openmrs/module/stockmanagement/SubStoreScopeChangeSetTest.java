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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SubStoreScopeChangeSetTest extends BaseModuleContextSensitiveTest {
	
	private static final String CHANGESET = "stockmanagement-2026-11-09-O3-6173";
	
	private static final String TRANSFER_UUID = "aaaaaaaa-1111-1111-1111-111111111111";
	
	private static final String RECEIPT_UUID = "bbbbbbbb-1111-1111-1111-111111111111";
	
	@BeforeEach
	public void setup() throws Exception {
		insertOperationType("Transfer Out", "transferout", "TRO", TRANSFER_UUID);
		insertOperationType("Receipt", "receipt", "REC", RECEIPT_UUID);
		insertScope(TRANSFER_UUID, "Main Store", true, false, false, "scope-1");
		insertScope(TRANSFER_UUID, "Main Pharmacy", true, false, false, "scope-2");
		insertScope(TRANSFER_UUID, "Dispensary", false, true, false, "scope-3");
		insertScope(RECEIPT_UUID, "Main Store", false, true, true, "scope-4");
		insertScope(RECEIPT_UUID, "Ward", true, true, false, "scope-5");
	}
	
	@Test
	public void subStoreScopeChangeSet_shouldCopyTheScopesOfTheExistingStockLocationTags() throws Exception {
		Element changeSet = getChangeSet(CHANGESET);
		assertNotNull(changeSet, "Changeset " + CHANGESET + " should exist");
		String precondition = text(changeSet, "sqlCheck");
		assertEquals(0, count(precondition));
		
		execute(text(changeSet, "sql"));
		
		assertEquals(2, count(subStoreScopes(TRANSFER_UUID, "")));
		assertEquals(1, count(subStoreScopes(TRANSFER_UUID, " AND s.is_source = true AND s.is_destination = false")));
		assertEquals(1, count(subStoreScopes(TRANSFER_UUID, " AND s.is_source = false AND s.is_destination = true")));
		assertEquals(2, count(subStoreScopes(TRANSFER_UUID, " AND s.voided = false")));
		assertEquals(0, count(subStoreScopes(RECEIPT_UUID, "")), "Voided scopes and other tags should not be copied");
		assertEquals(2, count(precondition));
	}
	
	private String subStoreScopes(String operationTypeUuid, String extraFilter) {
		return "SELECT COUNT(*) FROM stockmgmt_operation_type_location_scope s "
		        + "JOIN stockmgmt_stock_operation_type t ON t.stock_operation_type_id = s.stock_operation_type_id "
		        + "WHERE t.uuid = '" + operationTypeUuid + "' AND s.location_tag = 'Sub Store'" + extraFilter;
	}
	
	private void insertOperationType(String name, String operationType, String acronym, String uuid) throws Exception {
		execute("INSERT INTO stockmgmt_stock_operation_type(name, operation_type, description, has_source, source_type, "
		        + "has_destination, available_when_reserved, acronym, creator, date_created, voided, uuid) VALUES ('" + name
		        + "', '" + operationType + "', '" + name + "', true, 'Location', false, true, '" + acronym
		        + "', 1, now(), false, '" + uuid + "')");
	}
	
	private void insertScope(String operationTypeUuid, String tag, boolean source, boolean destination, boolean voided,
	        String uuid) throws Exception {
		execute("INSERT INTO stockmgmt_operation_type_location_scope(stock_operation_type_id, location_tag, is_source, "
		        + "is_destination, creator, date_created, voided, uuid) SELECT stock_operation_type_id, '" + tag + "', "
		        + source + ", " + destination + ", 1, now(), " + voided + ", '" + uuid
		        + "' FROM stockmgmt_stock_operation_type WHERE uuid = '" + operationTypeUuid + "'");
	}
	
	private Element getChangeSet(String id) throws Exception {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("liquibase.xml")) {
			assertNotNull(in, "liquibase.xml should be on the classpath");
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			Document document = factory.newDocumentBuilder().parse(in);
			NodeList changeSets = document.getElementsByTagName("changeSet");
			for (int i = 0; i < changeSets.getLength(); i++) {
				Element changeSet = (Element) changeSets.item(i);
				if (id.equals(changeSet.getAttribute("id"))) {
					return changeSet;
				}
			}
			return null;
		}
	}
	
	private String text(Element changeSet, String tag) {
		return changeSet.getElementsByTagName(tag).item(0).getTextContent().trim();
	}
	
	private void execute(String sql) throws Exception {
		Connection connection = getConnection();
		try (Statement statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}
	
	private int count(String sql) throws Exception {
		Connection connection = getConnection();
		try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
			resultSet.next();
			return resultSet.getInt(1);
		}
	}
}
