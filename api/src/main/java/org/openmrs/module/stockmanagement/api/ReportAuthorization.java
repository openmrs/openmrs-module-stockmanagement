package org.openmrs.module.stockmanagement.api;

import org.openmrs.User;
import org.openmrs.api.APIAuthenticationException;

/**
 * Authorization rules that are specific to stock management reports.
 */
public final class ReportAuthorization {

	public static final String INVENTORY_REPORTING_ROLE = "Inventory Reporting";

	private ReportAuthorization() {
	}

	public static boolean hasInventoryReportingRole(User user) {
		// ignoreSuperUser=true makes this an explicit role requirement, including for superusers.
		return user != null && user.hasRole(INVENTORY_REPORTING_ROLE, true);
	}

	public static void requireInventoryReportingRole(User user) {
		if (!hasInventoryReportingRole(user)) {
			throw new APIAuthenticationException("The Inventory Reporting role is required to access stock reports");
		}
	}
}
