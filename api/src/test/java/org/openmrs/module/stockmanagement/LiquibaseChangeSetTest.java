package org.openmrs.module.stockmanagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class LiquibaseChangeSetTest extends BaseModuleContextSensitiveTest {
	
	private static final String ADJUSTMENT_UUID = "11111111-1111-1111-1111-111111111111";
	
	private static final String ADJUSTMENT_MAIN_STORE_CHANGESET = "stockmanagement-2026-11-10-O3-6255";
	
	private static final String COUNT_MAIN_STORE_SCOPES = "SELECT COUNT(*) FROM stockmgmt_operation_type_location_scope s "
	        + "JOIN stockmgmt_stock_operation_type t ON t.stock_operation_type_id = s.stock_operation_type_id "
	        + "WHERE t.uuid = '" + ADJUSTMENT_UUID + "' AND s.location_tag = 'Main Store' AND s.is_source = true";
	
	@Before
	public void setup() throws Exception {
		execute("INSERT INTO stockmgmt_stock_operation_type(name, operation_type, description, has_source, source_type, "
		        + "has_destination, available_when_reserved, acronym, creator, date_created, voided, uuid) "
		        + "VALUES ('Adjustment', 'adjustment', 'Adjustment', true, 'Location', false, true, 'ADJ', 1, now(), false, '"
		        + ADJUSTMENT_UUID + "')");
	}
	
	@Test
	public void adjustmentMainStoreScopeChangeSet_shouldAddTheScopeOnlyWhenItIsMissing() throws Exception {
		Element changeSet = getChangeSet(ADJUSTMENT_MAIN_STORE_CHANGESET);
		assertNotNull("Changeset " + ADJUSTMENT_MAIN_STORE_CHANGESET + " should exist", changeSet);
		String precondition = text(changeSet, "sqlCheck");
		String insert = text(changeSet, "sql");
		
		assertEquals(0, count(COUNT_MAIN_STORE_SCOPES));
		assertEquals(0, count(precondition));
		
		execute(insert);
		assertEquals(1, count(COUNT_MAIN_STORE_SCOPES));
		assertEquals(1, count(precondition));
		
		execute(insert);
		assertEquals("Running the insert again should not add a second scope", 1, count(COUNT_MAIN_STORE_SCOPES));
	}
	
	@Test
	public void adjustmentMainStoreScopeChangeSet_shouldUseTheAdjustmentOperationTypeUuid() throws Exception {
		Element changeSet = getChangeSet(ADJUSTMENT_MAIN_STORE_CHANGESET);
		assertNotNull("Changeset " + ADJUSTMENT_MAIN_STORE_CHANGESET + " should exist", changeSet);
		assertTrue(text(changeSet, "sql").contains("'" + ADJUSTMENT_UUID + "'"));
		assertTrue(text(changeSet, "sqlCheck").contains("'" + ADJUSTMENT_UUID + "'"));
	}
	
	private Element getChangeSet(String id) throws Exception {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("liquibase.xml")) {
			assertNotNull("liquibase.xml should be on the classpath", in);
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
