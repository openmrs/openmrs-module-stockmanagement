package org.openmrs.module.stockmanagement.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.LocationTag;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.stockmanagement.StockLocationTags;

import java.util.Date;

public class LocationTagsSynchronize implements StartupTask {
	
	private Log log = LogFactory.getLog(this.getClass());
	
	private LocationTag ensureMainStore(LocationService locationService) {
		LocationTag mainStore = locationService.getLocationTagByName(StockLocationTags.MAIN_STORE_LOCATION_TAG);
		if (mainStore == null) {
			log.debug("Created main store tag");
			mainStore = new LocationTag();
			mainStore.setUuid("e539ebca-2899-11ed-bdcb-507b9dea1806");
			mainStore.setName(StockLocationTags.MAIN_STORE_LOCATION_TAG);
			mainStore.setDescription("A location which is the main facility stock holding area");
			mainStore.setDateCreated(new Date());
			mainStore.setCreator(Context.getAuthenticatedUser());
			mainStore = locationService.saveLocationTag(mainStore);
			
		}
		return mainStore;
	}
	
	private LocationTag ensureMainPharmacy(LocationService locationService) {
		LocationTag mainPharmacy = locationService.getLocationTagByName(StockLocationTags.MAIN_PHARMACY_LOCATION_TAG);
		if (mainPharmacy == null) {
			log.debug("Created main pharmacy tag");
			mainPharmacy = new LocationTag();
			mainPharmacy.setUuid("89a80c4d-2899-11ed-bdcb-507b9dea1806");
			mainPharmacy.setName(StockLocationTags.MAIN_PHARMACY_LOCATION_TAG);
			mainPharmacy.setDescription("Main pharmacy location.");
			mainPharmacy.setDateCreated(new Date());
			mainPharmacy.setCreator(Context.getAuthenticatedUser());
			mainPharmacy = locationService.saveLocationTag(mainPharmacy);
		}
		return mainPharmacy;
	}
	
	private LocationTag ensureDispensary(LocationService locationService) {
		LocationTag dispensary = locationService.getLocationTagByName(StockLocationTags.DISPENSARY_LOCATION_TAG);
		if (dispensary == null) {
			log.debug("Created dispensary tag");
			dispensary = new LocationTag();
			dispensary.setUuid("fe7c970f-2aba-11ed-ba4a-507b9dea1806");
			dispensary.setName(StockLocationTags.DISPENSARY_LOCATION_TAG);
			dispensary.setDescription("A dispensary.");
			dispensary.setDateCreated(new Date());
			dispensary.setCreator(Context.getAuthenticatedUser());
			dispensary = locationService.saveLocationTag(dispensary);
		}
		return dispensary;
	}
	
	/**
	 * Makes sure the location tags the module relies on exist. Which locations carry these tags is
	 * left to the implementation (for example through Initializer or the location administration
	 * pages); this task does not create or tag any location.
	 */
	@Override
	public void execute() {
		log.debug("Checking that the main store, main pharmacy and dispensary location tags exist");
		try {
			LocationService locationService = Context.getLocationService();
			ensureMainStore(locationService);
			ensureMainPharmacy(locationService);
			ensureDispensary(locationService);
		}
		catch (Exception exception) {
			log.error("Error while synchronizing location tags", exception);
		}
	}
	
	@Override
	public int getPriority() {
		return 90;
	}
}
