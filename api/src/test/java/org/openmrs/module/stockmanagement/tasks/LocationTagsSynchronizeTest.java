package org.openmrs.module.stockmanagement.tasks;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.StockLocationTags;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LocationTagsSynchronizeTest extends BaseModuleContextSensitiveTest {
	
	private static final String MAIN_STORE_LOCATION_UUID = "19dbe2c0-289d-11ed-bdcb-507b9dea1806";
	
	private static final String MAIN_PHARMACY_LOCATION_UUID = "7f65d926-57d6-4402-ae10-a5b3bcbf7986";
	
	private LocationService locationService;
	
	@Before
	public void setup() {
		locationService = Context.getLocationService();
	}
	
	@Test
	public void execute_shouldCreateTheStockLocationTags() {
		new LocationTagsSynchronize().execute();
		
		assertNotNull(locationService.getLocationTagByName(StockLocationTags.MAIN_STORE_LOCATION_TAG));
		assertNotNull(locationService.getLocationTagByName(StockLocationTags.MAIN_PHARMACY_LOCATION_TAG));
		assertNotNull(locationService.getLocationTagByName(StockLocationTags.DISPENSARY_LOCATION_TAG));
	}
	
	@Test
	public void execute_shouldNotCreateAnyLocation() {
		int locationsBefore = locationService.getAllLocations(true).size();
		
		new LocationTagsSynchronize().execute();
		
		assertEquals(locationsBefore, locationService.getAllLocations(true).size());
		assertNull(locationService.getLocationByUuid(MAIN_STORE_LOCATION_UUID));
		assertNull(locationService.getLocationByUuid(MAIN_PHARMACY_LOCATION_UUID));
		assertTrue(locationsTagged(StockLocationTags.MAIN_STORE_LOCATION_TAG).isEmpty());
		assertTrue(locationsTagged(StockLocationTags.MAIN_PHARMACY_LOCATION_TAG).isEmpty());
	}
	
	@Test
	public void execute_shouldNotTagLocationsBecauseOfTheirName() {
		Location pharmacy = saveLocation("Outpatient Pharmacy");
		Location store = saveLocation("Main Store");
		
		new LocationTagsSynchronize().execute();
		
		assertTrue(hasNoTags(locationService.getLocation(pharmacy.getLocationId())));
		assertTrue(hasNoTags(locationService.getLocation(store.getLocationId())));
	}
	
	@Test
	public void execute_shouldLeaveLocationsTaggedByTheImplementationAlone() {
		LocationTag mainStoreTag = new LocationTag(StockLocationTags.MAIN_STORE_LOCATION_TAG, "Configured by the site");
		mainStoreTag = locationService.saveLocationTag(mainStoreTag);
		Location warehouse = new Location();
		warehouse.setName("Central Warehouse");
		warehouse.addTag(mainStoreTag);
		warehouse = locationService.saveLocation(warehouse);
		int locationsBefore = locationService.getAllLocations(true).size();
		
		new LocationTagsSynchronize().execute();
		
		assertEquals(mainStoreTag.getUuid(),
		    locationService.getLocationTagByName(StockLocationTags.MAIN_STORE_LOCATION_TAG).getUuid());
		List<Location> mainStores = locationsTagged(StockLocationTags.MAIN_STORE_LOCATION_TAG);
		assertEquals(1, mainStores.size());
		assertEquals(warehouse.getUuid(), mainStores.get(0).getUuid());
		assertEquals(locationsBefore, locationService.getAllLocations(true).size());
	}
	
	@Test
	public void execute_shouldNotDuplicateTheTagsWhenRunAgain() {
		new LocationTagsSynchronize().execute();
		int tagsAfterFirstRun = locationService.getAllLocationTags(true).size();
		
		new LocationTagsSynchronize().execute();
		
		assertEquals(tagsAfterFirstRun, locationService.getAllLocationTags(true).size());
	}
	
	@Test
	public void liquibase_shouldNotInsertLocations() throws Exception {
		String changeLog;
		try (InputStream stream = getClass().getClassLoader().getResourceAsStream("liquibase.xml")) {
			assertNotNull(stream);
			changeLog = IOUtils.toString(stream, StandardCharsets.UTF_8);
		}
		
		assertFalse(Pattern.compile("insert\\s+into\\s+location\\s*\\(", Pattern.CASE_INSENSITIVE)
		        .matcher(changeLog).find());
		assertFalse(Pattern.compile("<insert\\s+tableName=\"location\"", Pattern.CASE_INSENSITIVE)
		        .matcher(changeLog).find());
		assertFalse(changeLog.contains(MAIN_STORE_LOCATION_UUID));
		assertFalse(changeLog.contains(MAIN_PHARMACY_LOCATION_UUID));
	}
	
	private Location saveLocation(String name) {
		Location location = new Location();
		location.setName(name);
		return locationService.saveLocation(location);
	}
	
	private boolean hasNoTags(Location location) {
		return location.getTags() == null || location.getTags().isEmpty();
	}
	
	private List<Location> locationsTagged(String tagName) {
		LocationTag tag = locationService.getLocationTagByName(tagName);
		return locationService.getLocationsHavingAnyTag(Collections.singletonList(tag));
	}
}
