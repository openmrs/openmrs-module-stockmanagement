package org.openmrs.module.stockmanagement;

public class StockLocationTags {
	
	public static final String MAIN_STORE_LOCATION_TAG = "Main Store";
	
	public static final String MAIN_PHARMACY_LOCATION_TAG = "Main Pharmacy";
	
	public static final String DISPENSARY_LOCATION_TAG = "Dispensary";
	
	public static final String SUB_STORE_LOCATION_TAG = "Sub Store";
	
	/**
	 * Used when the stockmanagement.locationTags global property is not set.
	 */
	public static final String DEFAULT_STOCK_LOCATION_TAGS = MAIN_STORE_LOCATION_TAG + "," + MAIN_PHARMACY_LOCATION_TAG + ","
	        + DISPENSARY_LOCATION_TAG + "," + SUB_STORE_LOCATION_TAG;
}
