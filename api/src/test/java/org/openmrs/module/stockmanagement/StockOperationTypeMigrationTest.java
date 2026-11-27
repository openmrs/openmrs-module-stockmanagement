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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.DirectoryResourceAccessor;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Runs the changesets that move operation types from a table to the {@link StockOperationType} enum
 * against real databases that hold rows in the old shape. Skipped when Docker is not available.
 */
public class StockOperationTypeMigrationTest {
	
	private static final String CHANGE_SET_ID = "id=\"stockmanagement-O3-6310-";
	
	private static final String CHANGE_SET_END = "</changeSet>";
	
	private static final int RECEIPT_ID = StockOperationType.RECEIPT.ordinal() + 1;
	
	private static final int STOCK_ISSUE_ID = StockOperationType.STOCK_ISSUE.ordinal() + 1;
	
	private static final int ADJUSTMENT_ID = StockOperationType.ADJUSTMENT.ordinal() + 1;
	
	static {
		// The Testcontainers version that comes with openmrs-api defaults to a Docker API version that
		// Docker 29 and later reject, which would silently skip these tests
		if (System.getProperty("api.version") == null) {
			System.setProperty("api.version", "1.41");
		}
	}
	
	@TempDir
	Path changeLogDirectory;
	
	@Test
	public void shouldUpgradeExistingDataOnMySQL() throws Exception {
		shouldUpgradeExistingData(new MySQLContainer<>(DockerImageName.parse("mysql:8.0")));
	}
	
	@Test
	public void shouldUpgradeExistingDataOnMariaDB() throws Exception {
		shouldUpgradeExistingData(
		    new MySQLContainer<>(DockerImageName.parse("mariadb:10.11").asCompatibleSubstituteFor("mysql")));
	}
	
	@Test
	public void shouldUpgradeExistingDataOnPostgreSQL() throws Exception {
		shouldUpgradeExistingData(newPostgreSQL());
	}
	
	@Test
	public void shouldStopWhenExistingDataUsesAnUnknownOperationType() throws Exception {
		assumeTrue(DockerClientFactory.instance().isDockerAvailable());
		try (JdbcDatabaseContainer<?> database = newPostgreSQL()) {
			database.start();
			try (Connection connection = database.createConnection("")) {
				createOldSchemaWithData(connection);
				execute(connection, "INSERT INTO stockmgmt_stock_operation_type VALUES (50, 'erequisition')",
				    "INSERT INTO stockmgmt_stock_operation (stock_operation_id, operation_type_id) VALUES (50, 50)");
				
				assertThrows(LiquibaseException.class, () -> runChangeSets(connection));
			}
		}
	}
	
	@Test
	public void shouldFillTheColumnWhenAnEarlierRunOnlyAddedIt() throws Exception {
		assumeTrue(DockerClientFactory.instance().isDockerAvailable());
		try (JdbcDatabaseContainer<?> database = newPostgreSQL()) {
			database.start();
			try (Connection connection = database.createConnection("")) {
				createOldSchemaWithData(connection);
				execute(connection, "ALTER TABLE stockmgmt_stock_operation ADD COLUMN operation_type VARCHAR(50)");
				
				runChangeSets(connection);
				
				assertEquals("RECEIPT", queryString(connection,
				    "SELECT operation_type FROM stockmgmt_stock_operation WHERE stock_operation_id = " + RECEIPT_ID));
			}
		}
	}
	
	private void shouldUpgradeExistingData(JdbcDatabaseContainer<?> container) throws Exception {
		assumeTrue(DockerClientFactory.instance().isDockerAvailable());
		try (JdbcDatabaseContainer<?> database = container) {
			database.start();
			try (Connection connection = database.createConnection("")) {
				createOldSchemaWithData(connection);
				
				runChangeSets(connection);
				execute(connection, "DELETE FROM DATABASECHANGELOG");
				runChangeSets(connection);
				
				for (StockOperationType type : StockOperationType.values()) {
					int id = type.ordinal() + 1;
					assertEquals(type.name(), queryString(connection,
					    "SELECT operation_type FROM stockmgmt_stock_operation WHERE stock_operation_id = " + id));
					assertEquals(type.name(),
					    queryString(connection, "SELECT operation_type FROM stockmgmt_user_role_scope_operation_type"
					            + " WHERE user_role_scope_operation_type_id = " + id));
				}
				
				// the old column and its foreign key no longer constrain new rows, and the new column is required
				execute(connection,
				    "INSERT INTO stockmgmt_stock_operation (stock_operation_id, operation_type) VALUES (100, 'RECEIPT')",
				    "INSERT INTO stockmgmt_stock_operation (stock_operation_id, operation_type_id, operation_type)"
				            + " VALUES (101, 999, 'RECEIPT')");
				assertThrows(SQLException.class, () -> execute(connection,
				    "INSERT INTO stockmgmt_stock_operation (stock_operation_id, operation_type_id) VALUES (102, 1)"));
				
				assertEquals("Main Store", locationTags(connection, "stockissue.sourceLocationTags"));
				assertEquals("Dispensary,Main Pharmacy", locationTags(connection, "stockissue.destinationLocationTags"));
				assertEquals("Site Rule", locationTags(connection, "receipt.destinationLocationTags"));
				assertNull(locationTags(connection, "adjustment.sourceLocationTags"));
			}
		}
	}
	
	private PostgreSQLContainer<?> newPostgreSQL() {
		return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"));
	}
	
	/**
	 * Creates the tables the changesets touch as they are in 3.x, with one stock operation and one user
	 * role scope entry per operation type, plus location tag rules.
	 */
	private void createOldSchemaWithData(Connection connection) throws SQLException {
		execute(connection,
		    "CREATE TABLE global_property (property VARCHAR(255) PRIMARY KEY, property_value TEXT, description TEXT,"
		            + " uuid CHAR(38) NOT NULL)",
		    "CREATE TABLE stockmgmt_stock_operation_type (stock_operation_type_id INT PRIMARY KEY,"
		            + " operation_type VARCHAR(255))",
		    "CREATE TABLE stockmgmt_stock_operation (stock_operation_id INT PRIMARY KEY, operation_type_id INT NOT NULL,"
		            + " CONSTRAINT stockmgmt_stock_operation_operation_type_fk FOREIGN KEY (operation_type_id)"
		            + " REFERENCES stockmgmt_stock_operation_type (stock_operation_type_id))",
		    "CREATE TABLE stockmgmt_user_role_scope_operation_type (user_role_scope_operation_type_id INT PRIMARY KEY,"
		            + " operation_type_id INT NOT NULL,"
		            + " CONSTRAINT stockmgmt_user_role_scope_operation_type_on_operation_type_fk"
		            + " FOREIGN KEY (operation_type_id)"
		            + " REFERENCES stockmgmt_stock_operation_type (stock_operation_type_id))",
		    "CREATE TABLE stockmgmt_operation_type_location_scope (stock_operation_type_location_scope_id INT PRIMARY KEY,"
		            + " stock_operation_type_id INT, location_tag VARCHAR(50) NOT NULL, is_source BOOLEAN NOT NULL,"
		            + " is_destination BOOLEAN NOT NULL, voided BOOLEAN)");
		for (StockOperationType type : StockOperationType.values()) {
			int id = type.ordinal() + 1;
			execute(connection,
			    "INSERT INTO stockmgmt_stock_operation_type VALUES (" + id + ", '" + type.getOperationType() + "')",
			    "INSERT INTO stockmgmt_stock_operation (stock_operation_id, operation_type_id) VALUES (" + id + ", " + id
			            + ")",
			    "INSERT INTO stockmgmt_user_role_scope_operation_type VALUES (" + id + ", " + id + ")");
		}
		execute(connection,
		    "INSERT INTO stockmgmt_operation_type_location_scope VALUES (1, " + STOCK_ISSUE_ID
		            + ", 'Main Store', true, false, false)",
		    "INSERT INTO stockmgmt_operation_type_location_scope VALUES (2, " + STOCK_ISSUE_ID
		            + ", 'Main Pharmacy', false, true, false)",
		    "INSERT INTO stockmgmt_operation_type_location_scope VALUES (3, " + STOCK_ISSUE_ID
		            + ", 'Dispensary', false, true, NULL)",
		    "INSERT INTO stockmgmt_operation_type_location_scope VALUES (4, " + RECEIPT_ID
		            + ", 'Main Store', false, true, false)",
		    "INSERT INTO stockmgmt_operation_type_location_scope VALUES (5, " + ADJUSTMENT_ID
		            + ", 'Voided Tag', true, false, true)",
		    // a property the site already set must not be overwritten
		    "INSERT INTO global_property VALUES ('stockmanagement.receipt.destinationLocationTags', 'Site Rule', NULL,"
		            + " 'existing-property')");
	}
	
	private void runChangeSets(Connection connection) throws Exception {
		String changeLog;
		try (InputStream stream = getClass().getClassLoader().getResourceAsStream("liquibase.xml")) {
			changeLog = IOUtils.toString(stream, StandardCharsets.UTF_8);
		}
		int rootStart = changeLog.indexOf("<databaseChangeLog");
		String root = changeLog.substring(rootStart, changeLog.indexOf('>', rootStart) + 1);
		List<String> changeSets = new ArrayList<>();
		int start = changeLog.indexOf("<changeSet ");
		while (start >= 0) {
			int end = changeLog.indexOf(CHANGE_SET_END, start) + CHANGE_SET_END.length();
			if (changeLog.substring(start, changeLog.indexOf('>', start)).contains(CHANGE_SET_ID)) {
				changeSets.add(changeLog.substring(start, end));
			}
			start = changeLog.indexOf("<changeSet ", end);
		}
		assertEquals(9, changeSets.size());
		Files.write(changeLogDirectory.resolve("changelog.xml"),
		    (root + String.join("\n", changeSets) + "</databaseChangeLog>").getBytes(StandardCharsets.UTF_8));
		
		Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
		try (DirectoryResourceAccessor resources = new DirectoryResourceAccessor(changeLogDirectory)) {
			new Liquibase("changelog.xml", resources, database).update(new Contexts());
		}
		// Liquibase leaves the connection in a transaction, which PostgreSQL aborts on the first failed statement
		connection.setAutoCommit(true);
	}
	
	private String locationTags(Connection connection, String setting) throws SQLException {
		return queryString(connection,
		    "SELECT property_value FROM global_property WHERE property = 'stockmanagement." + setting + "'");
	}
	
	private String queryString(Connection connection, String sql) throws SQLException {
		try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
			return result.next() ? result.getString(1) : null;
		}
	}
	
	private void execute(Connection connection, String... statements) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			for (String sql : statements) {
				statement.execute(sql);
			}
		}
	}
}
