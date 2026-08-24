package org.openmrs.module.stockmanagement.api;

import org.junit.Test;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.util.RoleConstants;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReportAuthorizationTest {

	@Test
	public void hasInventoryReportingRole_shouldRequireExplicitRole() {
		User user = new User();
		user.addRole(new Role(RoleConstants.SUPERUSER));

		assertFalse(ReportAuthorization.hasInventoryReportingRole(user));

		user.addRole(new Role(ReportAuthorization.INVENTORY_REPORTING_ROLE));

		assertTrue(ReportAuthorization.hasInventoryReportingRole(user));
	}
}
